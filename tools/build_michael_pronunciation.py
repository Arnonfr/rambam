"""Prepare offline Hebrew word pronunciations for the bundled Rambam corpus.

Run in an isolated environment with phonikud==0.4.1, phonikud-onnx==1.0.6
and onnxruntime. Source vowels remain authoritative. The neural model supplies
stress, prefix and vocal-shva hints; nothing changes the reader's source text.
"""
import argparse
import hashlib
import json
import re
import sqlite3
import time
import unicodedata
from pathlib import Path

import onnxruntime as ort
from phonikud import phonemize
from phonikud_onnx.model import OnnxModel

ROOT = Path(__file__).resolve().parents[1]
MARKS = re.compile(r"[\u0591-\u05c7]")
WORDS = re.compile(r"[א-ת][א-ת\u0591-\u05c7׳״'\"]*")
def key(text):
    return re.sub(r"[׳״'\"\u0591-\u05af\u05bd\u05bf\u05c0\u05c4\u05c5]", "", unicodedata.normalize("NFD", text))

LEXICON={key(w):ipa for w,ipa in {"כָּל":"kˈol", "לִקְבֹּעַ":"likbˈoa", "אֲפִלּוּ":"ʔafˈilu", "וַאֲפִלּוּ":"vaʔafˈilu", "וּמְחַזֵּר":"umeχazˈeʁ",
    # Sentence context (build_michael_context.py) mis-stressed these frequent words in הלכות מכירה.
    "קָנָה":"kanˈa", "וּקְנֵה":"ukˈne", "קְנֵה":"knˈe", "הַמִּקָּח":"hamikˈaχ", "מִקָּח":"mikˈaχ", "נָתַן":"natˈan", "בִּרְשׁוּת":"biʁʃˈut",
    "שֶׁמָּא":"ʃemˈa", "מֻתָּר":"mutˈaʁ", "הַפִּקָּדוֹן":"hapikadˈon", "הָאִשָּׁה":"haʔiʃˈa", "בִּשְׁעַת":"biʃʔˈat", "קָנוּ":"kanˈu",
    "הֲרֵינִי":"haʁenˈi", "מֵתָה":"metˈa", "מוֹצִיאִין":"motsiʔˈin", "אָנוּ":"ʔanˈu", "לָמַדְתָּ":"lamadtˈa", "רָצָה":"ʁatsˈa",
    "וּבֵית":"uvˈet", "פְּלוֹנִי":"plonˈi", "אַמַּת":"ʔamˈat", "לְהוֹצִיאוֹ":"lehotsiʔˈo", "הוֹדִיעוֹ":"hodiʔˈo", "וּמִי":"umˈi",
    "אַחַד":"ʔaχˈad", "הַוָּלָד":"havalˈad", "וָלָד":"valˈad", "לְהִתְיַחֵד":"lehitjaχˈed", "לִקַּח":"likˈaχ", "עֲלִיָּה":"ʔalijˈa",
    "דִּינָרִים":"dinaʁˈim", "דִּינָרִין":"dinaʁˈin", "גָּמַר":"ɡamˈaʁ", "אוֹכֵל":"ʔoχˈel"}.items()}

def glyphs(text):
    out=[]
    for c in unicodedata.normalize("NFD", text):
        if unicodedata.combining(c) or c=="|":
            if out: out[-1][1].append(c)
        else: out.append([c, []])
    return out

def enhance(source, predicted):
    a,b=glyphs(source),glyphs(predicted)
    assert [g[0] for g in a]==[g[0] for g in b], (source,predicted)
    if not any(c in source for c in "ְֱֲֳִֵֶַָׇֹֻ"):
        return predicted
    # Qamats qatan is a phonetic distinction that the printed qamats may omit.
    for (_,m),(_,n) in zip(a,b):
        if "\u05c7" in n and "\u05b8" in m: m[m.index("\u05b8")]="\u05c7"
    return "".join(c+"".join(m)+"".join(p for p in n if p in ["\u05ab","\u05bd","|"]
                 and p not in m and (p!="\u05bd" or "\u05b0" in m)) for (c,m),(_,n) in zip(a,b))

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--model",type=Path,required=True)
    parser.add_argument("--cache",type=Path,required=True)
    parser.add_argument("--seed",type=Path)
    args=parser.parse_args()
    conn=sqlite3.connect(ROOT/"app/src/main/assets/database/rambam.db")
    texts=[r[0] for r in conn.execute("select textWithNikud from halachot")]
    texts += [r[0] for r in conn.execute("select titleHebrew from content_sections")]
    extra=json.loads((ROOT/"app/src/main/assets/sefer_nashim.json").read_text())
    for section in extra["sections"]:
        for chapter in section["chapters"]:
            texts += [h["textWithNikud"] for h in chapter["halachot"]]
    texts += ["פרק הלכות אפס אחד שניים שלושה ארבעה חמישה שישה שבעה שמונה תשעה עשרה עשר עשרים שלושים ארבעים חמישים שישים שבעים שמונים תשעים ופרק"]
    # Numeric chapter labels are expanded by the Android engine into these words.
    texts += ["ו"+w for w in texts[-1].split()]
    vocab={key(m.group()) for t in texts for m in WORDS.finditer(t.replace('־',' '))}
    bare={MARKS.sub("",w) for w in vocab}
    predictions={}
    if args.cache.exists():
        for line in args.cache.read_text().splitlines():
            k,v=json.loads(line); predictions[k]=v
    seed={}
    if args.seed:
        source=(args.seed/"text-pointed.txt").read_text()
        hints=(args.seed/"predicted-pronunciation.txt").read_text()
        for a,b in zip(source.split(),hints.split()):
            a=key(a.strip(".,;:!?")); b=b.strip(".,;:!?")
            if MARKS.sub("",a)==re.sub(r"[\u0591-\u05c7|]","",b): seed[a]=b
    remaining=sorted(bare-predictions.keys())
    opts=ort.SessionOptions();opts.intra_op_num_threads=2;opts.inter_op_num_threads=1
    model=OnnxModel(str(args.model),session=ort.InferenceSession(str(args.model),opts))
    args.cache.parent.mkdir(parents=True,exist_ok=True)
    started=time.time(); done=0; last=started
    with args.cache.open("a") as cache:
        batch=[]; length=0
        def infer():
            nonlocal done,last
            sentence=" ".join(batch)
            result=model.predict(sentence)[0].split()
            assert len(result)==len(batch)
            for word,pred in zip(batch,result):
                assert word==re.sub(r"[\u0591-\u05c7|]","",pred), (word,pred)
                predictions[word]=pred;cache.write(json.dumps([word,pred],ensure_ascii=False)+"\n")
            cache.flush();done+=len(batch)
            if time.time()-last>20:
                print(f"Pronunciation hints {done}/{len(remaining)} · {int(time.time()-started)}s",flush=True);last=time.time()
        for word in remaining:
            if length+len(word)+1>350 and batch: infer();batch=[];length=0
            batch.append(word);length+=len(word)+1
        if batch: infer()
    entries={}; rejected=[]
    for word in sorted(vocab):
        b=MARKS.sub("",word)
        if word in LEXICON:
            ipa=LEXICON[word]
        elif len(b)==1 and not any(unicodedata.combining(c) for c in word):
            ipa="" # Original halacha markers are navigation labels, not words.
        else:
            ipa=phonemize(enhance(word,seed.get(word,predictions[b])))
        entries[word]=ipa
    for word,prediction in predictions.items():
        entries.setdefault(word,phonemize(prediction) if len(word)>1 else "")
    output=ROOT/"app/src/main/assets/speech/michael-pronunciation.json"
    data={"schema":1,"pipeline":"Phonikud 0.4.1 + 1.0 int8 hints; source vowels; lexical corrections v1",
          "sourceSha256":hashlib.sha256((ROOT/"app/src/main/assets/database/rambam.db").read_bytes()).hexdigest(),"words":entries}
    output.write_text(json.dumps(data,ensure_ascii=False,separators=(",",":")))
    print(f"Prepared {len(entries)} words; {output.stat().st_size} bytes; {int(time.time()-started)}s",flush=True)

if __name__=="__main__": main()
