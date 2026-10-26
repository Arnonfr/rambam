"""Review candidates against the existing scanned book, without automatic corrections.

Usage: python3 tools/audit_hayom_yom_text.py --pdf ../מחקר/hayom-yom-original-5716.pdf
Requires PyMuPDF. OCR differs from print and translations differ from the original;
the report is a candidate list, never a proof of text correctness.
"""
import argparse
import collections
import difflib
import json
import math
import re
from pathlib import Path
import fitz

ROOT = Path(__file__).resolve().parents[1]

def normalize(value):
    return re.sub("[^א-ת ]", "", re.sub(r"\s+", " ", value))

def grams(value):
    return collections.Counter(value[i:i + 3] for i in range(len(value) - 2))

def audit(pdf_path):
    corpus = json.loads((ROOT / "app/src/main/assets/hayom_yom.json").read_text())["entries"]
    with fitz.open(pdf_path) as pdf:
        pages = [normalize(page.get_text()) for page in pdf]
    page_grams = [grams(page) for page in pages]
    norms = [math.sqrt(sum(v * v for v in g.values())) or 1 for g in page_grams]
    report = []
    for key, entry in corpus.items():
        text = normalize(" ".join(entry["paragraphs"]))
        text_grams = grams(text)
        index = max(range(len(pages)), key=lambda i: sum(min(v, page_grams[i][k]) for k, v in text_grams.items()) / norms[i])
        original, displayed = pages[index].split(), text.split()
        operations = difflib.SequenceMatcher(None, original, displayed, autojunk=False).get_opcodes()
        changes = []
        for n, (tag, i, j, k, l) in enumerate(operations):
            if tag == "equal" or n == 0 or n + 1 >= len(operations):
                continue
            before, after = operations[n - 1], operations[n + 1]
            if before[0] == after[0] == "equal" and before[4] - before[3] >= 3 and after[4] - after[3] >= 3 and j-i <= 2 and l-k <= 2:
                changes.append(dict(originalOCR=" ".join(original[i:j]), app=" ".join(displayed[k:l]), context=" ".join(displayed[max(0,k-4):l+4])))
        report.append(dict(key=key, page=index+1, changes=changes))
    return report

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--pdf", type=Path, required=True)
    parser.add_argument("--output", type=Path, default=ROOT / "build/review/hayom-text-audit.json")
    args = parser.parse_args()
    report = audit(args.pdf)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
    print(f"Scanned {len(report)} entries; {sum(len(row['changes']) for row in report)} candidate OCR differences. No text changed.")
