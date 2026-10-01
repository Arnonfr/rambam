#!/usr/bin/env python3
"""Build the offline Sefer HaMitzvot text pack from Sefaria's public-domain export."""

import html
import json
import os
import re
import unicodedata
import urllib.request
from pathlib import Path


SOURCE_URL = (
    "https://storage.googleapis.com/sefaria-export/json/Halakhah/"
    "Sifrei%20Mitzvot/Sefer%20HaMitzvot/Sefer%20HaMitzvot/Hebrew/"
    "Sefer%20HaMitzvot%2C%20Warsaw%201883.json"
)
OUTPUT = Path(__file__).parent / "app/src/main/assets/sefer_hamitzvot_text.json"


def clean(value):
    if isinstance(value, list):
        return [clean(item) for item in value]
    if isinstance(value, dict):
        return {key: clean(item) for key, item in value.items()}
    if not isinstance(value, str):
        return value
    without_tags = re.sub(r"<[^>]+>", "", value)
    return unicodedata.normalize("NFC", html.unescape(without_tags)).strip()


def main():
    source_file = os.environ.get("SEFER_HAMITZVOT_SOURCE_FILE")
    if source_file:
        source = json.loads(Path(source_file).read_text(encoding="utf-8"))
    else:
        with urllib.request.urlopen(SOURCE_URL, timeout=30) as response:
            source = json.load(response)

    if source.get("license") != "Public Domain":
        raise RuntimeError("Unexpected Sefer HaMitzvot source license")

    text = source["text"]
    payload = {
        "version": "warsaw-1883-sefaria-2026-09",
        "title": "ספר המצוות לרמב״ם",
        "versionTitle": source["versionTitle"],
        "sourceUrl": SOURCE_URL,
        "license": source["license"],
        "digitizedBy": "Sefaria",
        "rambamIntroduction": clean(text["Introductions"]["The Rambam's Introduction"]),
        "roots": clean(text["Shorashim"]),
        "positive": clean(text["Positive Commandments"]),
        "negative": clean(text["Negative Commandments"]),
    }
    if len(payload["roots"]) != 14:
        raise RuntimeError("Expected 14 roots")
    if len(payload["positive"]) != 248 or len(payload["negative"]) != 365:
        raise RuntimeError("Expected 248 positive and 365 negative commandments")

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(
        json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        encoding="utf-8",
    )
    print(f"Wrote {OUTPUT} ({OUTPUT.stat().st_size:,} bytes)")


if __name__ == "__main__":
    main()
