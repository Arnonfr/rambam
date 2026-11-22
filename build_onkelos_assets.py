"""Bundle Targum Onkelos for שניים מקרא ואחד תרגום, one entry per Torah verse.

Source: Sefaria, "Sifsei Chachomim Chumash, Metsudah Publications, 2009"
(the vocalized Onkelos printed in standard Ashkenazi chumashim), CC BY-NC.
Keys match chumash_cache.json verse keys ("Genesis:1:1"). Nothing is written
unless every bundled Chumash verse has a translation.
"""
import html
import json
import re
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent
CHUMASH = ROOT / "app/src/main/assets/chumash_cache.json"
ASSET = ROOT / "app/src/main/assets/onkelos.json"
VERSION = "Sifsei Chachomim Chumash, Metsudah Publications, 2009"
BOOKS = ["Genesis", "Exodus", "Leviticus", "Numbers", "Deuteronomy"]


def clean(text):
    text = html.unescape(re.sub(r"<[^>]*>", "", text))
    return re.sub(r"\s+", " ", text.replace(" ", " ")).strip()


def fetch(book):
    url = ("https://www.sefaria.org/api/texts/Onkelos_" + book + "?context=0&commentary=0&pad=0"
           "&ven=&vhe=" + urllib.parse.quote(VERSION.replace(" ", "_")))
    request = urllib.request.Request(url, headers={"User-Agent": "DailyStudyApp/1.0"})
    with urllib.request.urlopen(request, timeout=90) as response:
        data = json.load(response)
    if data.get("heVersionTitle") != VERSION:
        raise ValueError(f"{book}: got version {data.get('heVersionTitle')!r}")
    return {f"{book}:{c}:{v}": clean(text)
            for c, chapter in enumerate(data["he"], 1)
            for v, text in enumerate(chapter, 1) if clean(text)}


def main():
    targum = {}
    for book in BOOKS:
        verses = fetch(book)
        print(book, len(verses), "verses", flush=True)
        targum.update(verses)
    chumash = json.loads(CHUMASH.read_text())
    needed = {key for parasha in chumash.values() for aliya in parasha["aliyot"]
              for key in aliya["verseKeys"]}
    missing = sorted(needed - targum.keys())
    if missing:
        raise ValueError(f"{len(missing)} verses without Onkelos, e.g. {missing[:5]}")
    ASSET.write_text(json.dumps({"version": VERSION, "license": "CC BY-NC", "verses": targum},
                                ensure_ascii=False, separators=(",", ":")) + "\n")
    print("Wrote", ASSET, len(targum), "verses")


if __name__ == "__main__":
    main()
