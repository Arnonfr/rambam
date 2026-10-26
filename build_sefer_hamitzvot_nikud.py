#!/usr/bin/env python3
"""Vocalize the Sefer HaMitzvot text pack with Dicta Nakdan (rabbinic genre).

Input: app/src/main/assets/sefer_hamitzvot_text.json (Warsaw 1883, Sefaria, public domain).
Output: ../מחקר/sefer-hamitzvot-nikud/ — a vocalized pack with the same structure,
plus a review list of words Dicta was not confident about.

Conventions match the app's Mishneh Torah text: no meteg, no qamatz qatan sign,
verse quotations vocalized (Dicta marks them and uses the Masoretic text),
parenthetical printer references and abbreviations left unpointed.
The automatic vocalization is not proofread; the letters themselves are verified
to be identical to the source.
"""

import json
import re
import time
import unicodedata
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path


DICTA_URL = "https://nakdan-5-3.loadbalancer.dicta.org.il/api"
SOURCE = Path(__file__).parent / "app/src/main/assets/sefer_hamitzvot_text.json"
OUT_DIR = Path(__file__).parent.parent / "מחקר/sefer-hamitzvot-nikud"
CACHE_DIR = OUT_DIR / "dicta-cache"
SECTIONS = ["rambamIntroduction", "roots", "positive", "negative"]

MARKS = re.compile(r"[֑-ְ֯-ׇֽֿׁׂׅׄ]")
MARKS_RUN = re.compile(r"[\u0591-\u05AF\u05B0-\u05BD\u05BF\u05C1\u05C2\u05C4\u05C5\u05C7]*")
HOLAM_BEFORE_VAV = re.compile(r"([א-ת])([ְ-ּׁׂ]*)ֹ([ְ-ּׁׂ]*)ו(?![ְ-ׇֽׁׂ])")


def strip_marks(text):
    return MARKS.sub("", text)


def call_dicta(text):
    body = json.dumps({
        "task": "nakdan", "genre": "rabbinic", "data": text, "addmorph": False,
        "keepqq": False, "nodageshdefmem": False, "patachma": False, "keepmetagim": False,
    }).encode("utf-8")
    request = urllib.request.Request(DICTA_URL, data=body, headers={"Content-Type": "application/json"})
    for attempt in range(6):
        try:
            with urllib.request.urlopen(request, timeout=180) as response:
                return json.load(response)
        except Exception:
            if attempt == 5:
                raise
            time.sleep(5 * (attempt + 1))


def vocalize_words(text):
    key = f"{abs(hash_text(text))}.json"
    cached = CACHE_DIR / key
    if cached.exists():
        data = json.loads(cached.read_text(encoding="utf-8"))
        if data["source"] == text:
            return data["words"]
    words = []
    for word in call_dicta(text):
        if word["sep"] or not word["options"]:
            words.append({"t": word["word"]})
        else:
            words.append({
                "src": word["word"],
                "t": top_option(word["options"]).replace("|", ""),
                "confident": bool(word.get("fconfident")),
                "pasuk": bool(word.get("fpasuk")),
            })
    cached.write_text(json.dumps({"source": text, "words": words}, ensure_ascii=False), encoding="utf-8")
    return words


def top_option(options):
    first = options[0]
    return first if isinstance(first, str) else first[0]


def hash_text(text):
    import hashlib
    return int(hashlib.sha1(text.encode("utf-8")).hexdigest()[:15], 16)


def normalize_holam(word):
    # Dicta sometimes writes holam male as letter+holam followed by a bare vav.
    return HOLAM_BEFORE_VAV.sub(lambda m: m.group(1) + m.group(2) + m.group(3) + "וֹ", word)


def unpoint_parentheses(text):
    return re.sub(r"\([^()]*\)", lambda m: strip_marks(m.group(0)), text)


HOLAM, KUBUTZ, QAMATZ, QAMATZ_QATAN, DAGESH = "\u05B9", "\u05BB", "\u05B8", "\u05C7", "\u05BC"
HATAF_QAMATZ = "\u05B3"


def clusters(word):
    result = []
    for char in word:
        if MARKS.match(char) and result:
            result[-1] += char
        else:
            result.append(char)
    return result


def restore_spelling(source, pointed):
    """Map Dicta's defective spelling back onto the source's plene letters.

    A vav the source adds becomes holam male or shuruk in place of the holam/kubutz
    on the previous letter; an added yod (or a doubled consonantal vav) stays bare.
    Returns None when the letters cannot be aligned.
    """
    pointed_clusters = clusters(pointed)
    out, j = [], 0
    for position, letter in enumerate(source):
        # In "וו" after kubutz (מצווה = מְצֻוֶּה) the first vav is the shuruk, the second the consonant.
        mater_before_consonant = (
            letter == "ו" and source[position + 1:position + 2] == "ו" and out and KUBUTZ in out[-1]
        )
        if not mater_before_consonant and j < len(pointed_clusters) and pointed_clusters[j][0] == letter:
            out.append(pointed_clusters[j])
            j += 1
        elif letter == "ו" and out:
            previous = out[-1]
            if HOLAM in previous:
                out[-1] = previous.replace(HOLAM, "")
                out.append("ו" + HOLAM)
            elif KUBUTZ in previous:
                out[-1] = previous.replace(KUBUTZ, "")
                out.append("ו" + DAGESH)
            elif QAMATZ_QATAN in previous or HATAF_QAMATZ in previous:
                out[-1] = previous.replace(QAMATZ_QATAN, "").replace(HATAF_QAMATZ, "")
                out.append("ו" + HOLAM)
            else:
                out.append("ו")
        elif letter == "י" and out:
            out.append("י")
        elif not MARKS.match(letter) and not ("\u05D0" <= letter <= "\u05EA"):
            out.append(letter)
        else:
            return None
    if j != len(pointed_clusters):
        return None
    return "".join(out)


QUOTE_EQUIVALENTS = {("\"", "\u05F4"), ("'", "\u05F3"), ("\"", "\u201D"), ("\"", "\u201C"), ("'", "\u2019")}


def restore_punctuation(source, pointed):
    """Put back the source's gershayim/quote characters that Dicta normalizes to ASCII."""
    source = strip_marks(source)
    out, i = [], 0
    for char in pointed:
        if MARKS.match(char):
            out.append(char)
            continue
        if i < len(source) and char != source[i] and (char, source[i]) in QUOTE_EQUIVALENTS:
            char = source[i]
        out.append(char)
        i += 1
    return "".join(out)


BEGADKEFAT = "בגדכפת"
ACRONYM = re.compile(r"[\u05D0-\u05EA][\"'\u05F3\u05F4][\u05D0-\u05EA]")

# Words Dicta vocalizes consistently wrong in this text (source spelling -> vocalization).
FIXED_WORDS = {
    "מאכול": "מֵאֱכוֹל",
    "מהאמין": "מֵהַאֲמִין",
    "מלקות": "מַלְקוֹת",
    "מלקיות": "מַלְקִיּוֹת",
    "לאוין": "לָאוִין",
    "הלאוין": "הַלָּאוִין",
    "שבכללות": "שֶׁבִּכְלָלוֹת",
    "באר": "בֵּאֵר",
    "ובאר": "וּבֵאֵר",
    "אמרם": "אָמְרָם",
    "מאמרו": "מֵאָמְרוֹ",
    "מהקריב": "מֵהַקְרִיב",
    "באור": "בֵּאוּר",
    "ובאור": "וּבֵאוּר",  # always introduces a verse: "from His saying"
}
SPEAKER = {"יתעלה", "יתברך"}


def contextual_fix(source, previous, following):
    """Rambam idioms where the right reading depends on the neighbouring words."""
    # "והוא אמרו יתעלה" — His saying (אָמְרוֹ), not "they said".
    if source in {"אמרו", "ואמרו", "שאמרו"} and (following in SPEAKER or (source == "אמרו" and previous in {"הוא", "והוא", "זהו", "וזהו", "זו", "בזה", "ענין"})):
        return {"אמרו": "אָמְרוֹ", "ואמרו": "וְאָמְרוֹ", "שאמרו": "שֶׁאָמְרוֹ"}[source]
    if source == "אמרו" and previous == "ושם":
        return "אָמְרוּ"
    # העובר is the transgressor, except the fetus in "הוציא העובר את ידו".
    if source == "העובר" and following != "את":
        return "הָעוֹבֵר"
    if source == "עלה" and following == "וסבה":
        return "עִלָּה"
    if source in {"פועל", "שפועל"} and following == "המציאות":
        return {"פועל": "פּוֹעֵל", "שפועל": "שֶׁפּוֹעֵל"}[source]
    return None


def apply_dagesh_convention(word):
    """Prose convention of the app's Mishneh Torah: dagesh kal on every word-initial
    בגדכפת, and no conjunctive dagesh on other initial letters (shuruk excepted)."""
    if len(word) < 2 or not MARKS.match(word[1]):
        return word
    first, rest = word[0], word[1:]
    marks = MARKS_RUN.match(rest).group(0)
    tail = rest[len(marks):]
    if first in BEGADKEFAT and DAGESH not in marks:
        marks = DAGESH + marks
    elif first not in BEGADKEFAT and first != "ו" and DAGESH in marks:
        marks = marks.replace(DAGESH, "")
    return unicodedata.normalize("NFC", first + marks + tail)


def protect_divine_abbreviations(text):
    def protect(match):
        plain = strip_marks(match.group(0))
        return plain if re.fullmatch(r"[ובכלמש]?ה['׳]", plain) else match.group(0)
    return re.sub(r"[א-ת\u0591-\u05C7]+['׳]", protect, text)


def vocalize(text, location, review):
    words = vocalize_words(text)
    word_positions = [index for index, word in enumerate(words) if "src" in word]
    neighbours = {}
    for order, index in enumerate(word_positions):
        previous = words[word_positions[order - 1]]["src"] if order else ""
        following = words[word_positions[order + 1]]["src"] if order + 1 < len(word_positions) else ""
        neighbours[index] = (previous, following)

    parts = []
    for index, word in enumerate(words):
        value = word["t"]
        if "src" in word:
            source = word["src"]
            value = normalize_holam(value.replace("\u05BD", ""))
            restored = restore_spelling(source, value) if strip_marks(value) != source else value
            if restored is None:
                review.append({"location": location, "source": source, "nikud": value, "issue": "alignment"})
                restored = source
            restored = restored.replace(QAMATZ_QATAN, QAMATZ)
            if ACRONYM.search(source):
                restored = source
            elif not word["pasuk"]:
                fixed = contextual_fix(source, *neighbours[index]) or FIXED_WORDS.get(source)
                if fixed:
                    if fixed != restored:
                        review.append({"location": location, "source": source, "dicta": restored, "nikud": fixed, "issue": "corrected"})
                    restored = fixed
                restored = apply_dagesh_convention(restored)
            value = restored
        parts.append(value)
    result = restore_punctuation(text, unpoint_parentheses("".join(parts)))
    result = unicodedata.normalize("NFC", protect_divine_abbreviations(result))
    if strip_marks(result) != strip_marks(text):
        raise RuntimeError(f"Letters changed in {location}")
    return result


def collect(value, path, out):
    if isinstance(value, str):
        out.append((path, value))
    else:
        for index, item in enumerate(value):
            collect(item, path + [index], out)


def assign(root, path, value):
    for index in path[:-1]:
        root = root[index]
    root[path[-1]] = value


def label(section, path):
    if section == "rambamIntroduction":
        return f"הקדמה ¶{path[0] + 1}"
    name = {"roots": "שורש", "positive": "עשה", "negative": "לא תעשה"}[section]
    return f"{name} {path[0] + 1}" + (f" ¶{path[1] + 1}" if len(path) > 1 else "")


def main():
    CACHE_DIR.mkdir(parents=True, exist_ok=True)
    source = json.loads(SOURCE.read_text(encoding="utf-8"))
    jobs = []
    for section in SECTIONS:
        items = []
        collect(source[section], [], items)
        jobs += [(section, path, text) for path, text in items if text.strip()]

    def run(job):
        local_review = []
        return vocalize(job[2], label(job[0], job[1]), local_review), local_review

    with ThreadPoolExecutor(max_workers=3) as pool:
        outcomes = list(pool.map(run, jobs))
    results = [result for result, _ in outcomes]
    review = [entry for _, entries in outcomes for entry in entries]

    payload = dict(source)
    payload["version"] = source["version"] + "-nikud-dicta"
    payload["vocalization"] = {
        "tool": "Dicta Nakdan (rabbinic genre)",
        "url": "https://nakdan.dicta.org.il",
        "proofread": False,
        "license": "CC BY-NC-SA 4.0",
        "licenseUrl": "https://creativecommons.org/licenses/by-nc-sa/4.0/",
        "termsUrl": "https://dicta.org.il/tos",
        "changes": "החזרת כתיב המקור, תיקוני הקשר והסרת ניקוד מקיצורי שם ה׳; ללא הגהה מלאה",
        "conventions": "ללא מתג וקמץ קטן; פסוקים לפי המסורה; הפניות בסוגריים וראשי תיבות ללא ניקוד",
    }
    for section in SECTIONS:
        payload[section] = json.loads(json.dumps(source[section]))
    for (section, path, _), result in zip(jobs, results):
        assign(payload[section], path, result)

    (OUT_DIR / "sefer_hamitzvot_text_nikud.json").write_text(
        json.dumps(payload, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    (OUT_DIR / "review-corrections.json").write_text(
        json.dumps(review, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"{len(jobs)} paragraphs vocalized; {len(review)} corrections and alignment notes")


if __name__ == "__main__":
    main()
