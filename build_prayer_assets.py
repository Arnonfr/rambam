"""Fetch attributed Nusach Ari source sections; never silently mix nusachim.

Writes JSON to stdout for review/import. Source: Hebrew Wikisource's Torah Ohr,
not a claim that this is a proofread transcription of modern Tehillat Hashem.
"""
import concurrent.futures
import html
import json
import re
import subprocess
import sys
import unicodedata
import urllib.parse
from html.parser import HTMLParser
from bs4 import BeautifulSoup

SECTIONS = [
    ("morning", "השכמת הבוקר"),
    ("washing", "סדר נטילת ידיים"),
    ("blessings", "ברכות השחר"),
    ("offerings", "קרבנות"),
    ("psalms", "פסוקי דזמרה"),
    ("shema", "קריאת שמע וברכותיה לשחרית של חול"),
    ("amidah", "שמונה עשרה"),
    ("tachanun", "תחנון"),
    ("closing", "סיום שחרית לחול"),
    ("grace", "סדר ברכת המזון"),
    ("bedtime", "קריאת שמע שעל המטה"),
]


class Blocks(HTMLParser):
    def __init__(self):
        super().__init__()
        self.stack = []
        self.blocks = []
        self.current = []
        self.enabled = False
        self.root_depth = 0

    def flush(self):
        text = unicodedata.normalize("NFC", re.sub(r"\s+", " ", "".join(self.current))).strip()
        self.current = []
        if re.search(r"[א-ת]", text):
            self.blocks.append(text)

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if tag in ("br", "hr", "img", "meta", "link", "input"):
            if self.enabled and tag == "br":
                self.flush()
            return
        self.stack.append(tag)
        if "prp-pages-output" in attrs.get("class", ""):
            self.enabled = True
            self.root_depth = len(self.stack)
        if self.enabled and tag in ("p", "div", "h2", "h3", "h4"):
            self.flush()

    def handle_endtag(self, tag):
        if tag not in self.stack:
            return
        depth = len(self.stack) - self.stack[::-1].index(tag)
        if self.enabled and tag in ("p", "div", "h2", "h3", "h4"):
            self.flush()
        if self.enabled and depth == self.root_depth:
            self.enabled = False
        self.stack = self.stack[:depth - 1]

    def handle_data(self, data):
        if self.enabled and "style" not in self.stack and "script" not in self.stack:
            self.current.append(data)


def fetch(section):
    key, title = section
    page = "סידור תורה אור/" + title
    url = "https://he.wikisource.org/w/api.php?" + urllib.parse.urlencode({
        "action": "parse", "page": page, "prop": "text", "format": "json"
    }, quote_via=urllib.parse.quote)
    result = subprocess.run(["curl", "--fail", "-L", "--retry", "2", "--retry-all-errors", "--max-time", "30", "-s", url], capture_output=True, check=True)
    parsed = json.loads(result.stdout)["parse"]
    soup = BeautifulSoup(parsed["text"]["*"], "html.parser")
    root = soup.select_one(".mw-parser-output")
    for unwanted in root.select("table, .toplink, .pagenum, .mw-editsection, .noprint, style, script, .catlinks, sup, .reference, .tooltip, .TooltipSpan, .explain, .error, .mw-ext-cite-error, .mw-references-wrap, ol.references, span[style*='display:none'], span[style*='display: none']"):
        unwanted.decompose()
    blocks = []
    for node in root.select("p, div[style*=David], h2, h3, h4"):
        if node.find_parent("div", style=lambda value: value and "David" in value):
            continue
        text = unicodedata.normalize("NFC", node.get_text("", strip=False))
        text = re.sub(r"\s+", " ", text).strip()
        if re.search(r"[א-ת]", text) and not any(s in text for s in ("דף זה הוא חלק", "עריכה", "לנוסחים נוספים", "סידור תורה אור\\", "הערות שוליים")):
            # Keep long liturgy readable and anchors precise without changing words.
            blocks.extend(re.split(r"(?<=[׃:])\s+", text) if len(text) > 1200 else [text])
    if not blocks or sum(map(len, blocks)) < 50:
        raise ValueError("No transcribed source content for " + title)
    return {"id": key, "title": title, "source": "https://he.wikisource.org/wiki/" + urllib.parse.quote(page),
            "revision": parsed.get("revid"), "paragraphs": blocks}


if __name__ == "__main__":
    def available(section):
        try:
            return fetch(section)
        except (subprocess.CalledProcessError, KeyError, ValueError) as error:
            print("Unavailable: " + section[1] + " (" + type(error).__name__ + ")", file=sys.stderr)
            return None
    with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:
        sections = [s for s in pool.map(available, SECTIONS) if s is not None]
    if not sections:
        raise RuntimeError("No verified source sections downloaded")
    print(json.dumps({"edition": "סידור תורה אור — נוסח האר״י (חב״ד)",
                      "license": "CC BY-SA 4.0", "attribution": "תורמי ויקיטקסט העברי; בעל התניא",
                      "changes": "חלוקה לפסקאות, נרמול Unicode והסרת סימון HTML בלבד",
                      "sections": sections}, ensure_ascii=False, indent=2))
