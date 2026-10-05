"""Audit explicit diagram placeholders across the complete Mechon Mamre text.

Outputs references and brief identifying phrases, not a republication of the text.
The audit does not establish manuscript authorship or completeness across editions.
"""
import concurrent.futures
import html
import json
import re
import subprocess
from pathlib import Path

BASE = "https://mechon-mamre.org/i/"


def fetch(name):
    result = subprocess.run(
        ["curl", "--fail", "--retry", "2", "--max-time", "40", "-Ls", BASE + name],
        capture_output=True, check=True,
    )
    value = result.stdout.decode("utf-8")
    if "משנה תורה" not in value or "<TITLE>" not in value:
        raise ValueError("Unexpected response: " + name)
    return value


def plain(value):
    return re.sub(r"\s+", " ", html.unescape(re.sub(r"<[^>]+>", " ", value))).strip()


def audit(name):
    page = fetch(name)
    title = plain(re.search(r"<TITLE>(.*?)</TITLE>", page, re.I | re.S)[1])
    hits = []
    for match in re.finditer(r"\[(?:איור הושמט כאן|מקום הציור)\]", page):
        before = page[:match.start()]
        anchors = list(re.finditer(r'<A NAME="([^"]+)">', before, re.I))
        headings = list(re.finditer(r"<H[12][^>]*>(.*?)</H[12]>", before, re.I | re.S))
        previous_text = plain(before[-650:])
        labels = list(re.finditer(r"<B>([א-ת]+,[א-ת]+)</B>", before, re.I))
        hits.append({
            "source": BASE + name,
            "title": title,
            "chapterHeading": plain(headings[-1][1]) if headings else "",
            "anchor": anchors[-1][1] if anchors else "",
            "editionHalachaLabel": labels[-1][1] if labels else "",
            "identifyingPhrase": previous_text[-110:],
        })
    return name, hits


if __name__ == "__main__":
    index = fetch("0.htm")
    names = sorted({x[:2] + ".htm" for x in re.findall(r'HREF="([1-9a-e][0-9a-f]00\.htm)"', index, re.I)})
    findings, errors = [], []
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        jobs = {pool.submit(audit, name): name for name in names}
        for job in concurrent.futures.as_completed(jobs):
            try:
                name, hits = job.result()
                findings.extend(hits)
                print(name, "diagrams:", len(hits), flush=True)
            except Exception as exc:
                errors.append({"page": jobs[job], "error": str(exc)})
    output = {"edition": "Mechon Mamre unpointed complete section pages", "sectionsRequested": len(names),
              "sectionsSucceeded": len(names) - len(errors), "errors": errors,
              "references": sorted(findings, key=lambda x: (x["source"], x["anchor"]))}
    destination = Path("docs/rambam-diagram-audit.json")
    destination.parent.mkdir(exist_ok=True)
    destination.write_text(json.dumps(output, ensure_ascii=False, indent=2) + "\n")
    print("TOTAL", len(findings), "ERRORS", len(errors))
