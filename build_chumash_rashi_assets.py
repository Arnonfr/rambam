"""Refresh bundled Rashi atomically, preserving one entry per Torah verse."""
import concurrent.futures
import html
import json
import re
import time
import urllib.request
from pathlib import Path

ASSET = Path(__file__).parent / "app/src/main/assets/chumash_cache.json"


def clean(text):
    # Preserve bold dibbur hamatchil, but remove all other source markup.
    return html.unescape(re.sub(r"<(?!/?b\b)[^>]*>", "", text)).strip()


def fetch(ref, expected):
    url = "https://www.sefaria.org/api/texts/Rashi_on_" + ref.replace(" ", ".") + "?context=0"
    for attempt in range(3):
        try:
            request = urllib.request.Request(url, headers={"User-Agent": "DailyStudyApp/1.0"})
            with urllib.request.urlopen(request, timeout=45) as response:
                data = json.load(response)
            verses = data["he"]
            if data["sections"][0] != data["toSections"][0]:
                # Rashi can omit a chapter's trailing uncommented verses. Obtain
                # the Torah chapter boundaries before flattening, not afterwards.
                torah_url = url.replace("Rashi_on_", "")
                with urllib.request.urlopen(torah_url, timeout=45) as response:
                    torah = json.load(response)["he"]
                if len(verses) != len(torah):
                    raise ValueError(f"{ref}: chapter count mismatch")
                verses = [chapter + [[]] * (len(t) - len(chapter))
                          for chapter, t in zip(verses, torah)]
                verses = [verse for chapter in verses for verse in chapter]
            else:
                verses += [[]] * (expected - len(verses))
            if len(verses) != expected or any(not isinstance(v, list) for v in verses):
                raise ValueError(f"{ref}: expected {expected} verse groups, got {len(verses)}")
            return [[clean(comment) for comment in verse if clean(comment)] for verse in verses]
        except Exception:
            if attempt == 2:
                raise
            time.sleep(1)


def main():
    data = json.loads(ASSET.read_text())
    aliyot = [a for p in data.values() for a in p["aliyot"]]
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        jobs = {pool.submit(fetch, a["ref"], len(a["verses"])): a for a in aliyot}
        for job in concurrent.futures.as_completed(jobs):
            aliya = jobs[job]
            comments = job.result()
            for verse, rashi in zip(aliya["verses"], comments):
                verse["rashi"] = rashi
                for field in ("textWithTeamim", "textPlainNikud"):
                    verse[field] = clean(verse[field])
            print(aliya["ref"], sum(map(len, comments)), "comments", flush=True)
    # Nothing is written if any download or verse-alignment validation failed.
    ASSET.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n")


if __name__ == "__main__":
    main()
