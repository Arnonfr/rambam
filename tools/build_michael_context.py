"""Prepare sentence-context pronunciations for upcoming daily Rambam lessons.

build_michael_pronunciation.py predicts every word once, out of context. Here
Phonikud sees each real sentence, so stress, vocal shva and prefixes follow the
sentence. Source vowels stay authoritative (same `enhance` merge). Sentences
outside the prepared dates fall back to the word index in the app.

Run with the Phonikud environment (phonikud==0.4.1, phonikud-onnx==1.0.6):
  python tools/build_michael_context.py --model phonikud-1.0.int8.onnx --start 2026-10-22 --days 33
"""
import argparse
import datetime
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

from build_michael_pronunciation import LEXICON, enhance, key

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"
MARKS = re.compile(r"[֑-ׇ]")
# Same tokens and sentence ends as MichaelPronunciation.kt.
TOKEN = re.compile(r"[א-ת][א-ת֑-ׇ׳״'\"]*|[.,;:!?]")
ENDS = set(".:;?!")
QUOTES = re.compile(r"[׳״'\"]")
PREDICTED = re.compile(r"[א-ת][א-ת\u0591-\u05C7|׳״'\"]*")
NIKUD = r"ְ-ׇּׁׂ"
MARKER = re.compile(r"^\s*[א-ת]{1,3}\s+")  # halacha number (א, יא…), dropped as in DailySpeechText.lesson


def prepare(source):
    """DailySpeechText.prepare + speak."""
    s = re.sub(r"<[^>]+>", " ", source).replace("&nbsp;", " ").replace("&quot;", '"').replace("&amp;", "&")
    s = re.sub(r"[֑-ֽֿ֯׀ׅׄ]", "", s).replace("־", " ").replace("׃", ".")
    s = re.sub(r"\s*\([^()" + NIKUD + r"]*\)", " ", re.sub(r"\s+", " ", s).strip())
    return re.sub(r"\s+", " ", re.sub(r"[()\[\]]", " ", s)).strip()


def sentences(text):
    """Hebrew word tokens grouped up to a sentence end, as the app groups them."""
    group = []
    for token in TOKEN.findall(text):
        if token[0] in ENDS:
            if group: yield group
            group = []
        elif token[0] != ",":
            group.append(token)
    if group: yield group


def group_key(words):
    return hashlib.sha256(" ".join(key(w) for w in words).encode()).hexdigest()[:16]


def lessons(start, days):
    schedule = json.loads((ASSETS / "rambam_schedule.json").read_text())["days"]
    chapters = []
    for i in range(days):
        day = schedule.get(str(start + datetime.timedelta(days=i)))
        if day:
            chapters += [(c["sectionId"], c["chapterNumber"]) for c in day["one"] + day["three"]]
    return list(dict.fromkeys(chapters))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", type=Path, required=True)
    parser.add_argument("--start", type=datetime.date.fromisoformat, required=True)
    parser.add_argument("--days", type=int, default=31)
    args = parser.parse_args()
    words = json.loads((ASSETS / "speech/michael-pronunciation.json").read_text())["words"]
    conn = sqlite3.connect(ASSETS / "database/rambam.db")
    chapters = lessons(args.start, args.days)
    groups = {}
    for section, number in chapters:
        rows = conn.execute("select textWithNikud,textPlain from halachot where sectionId=? and chapterNumber=? "
                            "order by halachaNumber", (section, number)).fetchall()
        assert rows, (section, number)
        for pointed, plain in rows:
            for group in sentences(prepare(MARKER.sub("", pointed or plain, count=1))):
                groups.setdefault(group_key(group), group)

    opts = ort.SessionOptions(); opts.intra_op_num_threads = 4; opts.inter_op_num_threads = 1
    model = OnnxModel(str(args.model), session=ort.InferenceSession(str(args.model), opts))
    started = time.time()
    entries, changed, total, misaligned = {}, 0, 0, 0
    pending = list(groups.items())
    # Several neighbouring sentences per call: more context, fewer calls; well under the 512-token window.
    while pending:
        batch, length = [], 0
        while pending and (not batch or length + sum(map(len, pending[0][1])) < 300):
            batch.append(pending.pop(0)); length += sum(map(len, batch[-1][1])) + len(batch[-1][1])
        text = " ".join(" ".join(MARKS.sub("", w) for w in g) + "." for _, g in batch)
        predicted = [QUOTES.sub("", t) for t in PREDICTED.findall(model.predict(text)[0])]
        source = [w for _, g in batch for w in g]
        if [re.sub(r"[֑-ׇ|]", "", p) for p in predicted] != [QUOTES.sub("", MARKS.sub("", w)) for w in source]:
            misaligned += len(batch); continue
        at = 0
        for k, group in batch:
            ipa = []
            for word in group:
                w, p = key(word), predicted[at]; at += 1
                if w in LEXICON: value = LEXICON[w]
                elif len(MARKS.sub("", w)) == 1 and not any(unicodedata.combining(c) for c in w): value = ""
                else: value = phonemize(enhance(w, p))
                total += 1; changed += value != words.get(w)
                ipa.append(value)
            entries[k] = ipa
        if len(entries) % 500 < len(batch):
            print(f"{len(entries)}/{len(groups)} sentences · {int(time.time() - started)}s", flush=True)

    output = ASSETS / "speech/michael-context.json"
    data = {"schema": 1, "pipeline": "Phonikud 0.4.1 + 1.0 int8 in sentence context; source vowels",
            "dates": f"{args.start}+{args.days}d", "chapters": len(chapters),
            "sourceSha256": hashlib.sha256((ASSETS / "database/rambam.db").read_bytes()).hexdigest(),
            "sentences": entries}
    output.write_text(json.dumps(data, ensure_ascii=False, separators=(",", ":")))
    print(f"{len(chapters)} chapters, {len(entries)}/{len(groups)} sentences ({misaligned} misaligned, word index kept); "
          f"{changed}/{total} words differ from the word index; {output.stat().st_size} bytes; {int(time.time() - started)}s")


if __name__ == "__main__":
    main()
