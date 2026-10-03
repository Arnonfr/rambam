"""Import date-keyed Hayom Yom from Chabadtext, preserving source and revisions.

The corpus is GFDL-1.3-or-later; original transparent wiki source and the
unmodified license accompany the rendered text. Navigation is not study text.
"""
import html
import json
import re
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent
API = "https://text.chabadpedia.com/api.php"

def query(**args):
    url = API + "?" + urllib.parse.urlencode(dict(format="json", **args))
    request = urllib.request.Request(url, headers={"User-Agent": "curl/8.7.1"})
    for attempt in range(3):
        try:
            with urllib.request.urlopen(request, timeout=45) as response:
                return json.load(response)
        except (OSError, ValueError):
            if attempt == 2:
                raise

def clean(raw):
    raw = re.sub(r"\{\{כפי\|[^{}]+\}\}", "", raw)
    raw = re.sub(r"\{\{מונחון\|([^|{}]+)\|([^{}]+)\}\}", r"\1 (\2)", raw)
    raw = re.sub(r"<noinclude>.*?</noinclude>", "", raw, flags=re.S)
    raw = re.sub(r"<!--.*?-->", "", raw, flags=re.S)
    raw = re.sub(r"\[\[([^]|]+)\|([^]]+)\]\]", r"\2", raw)
    raw = re.sub(r"\[\[([^]]+)\]\]", r"\1", raw)
    raw = re.sub(r"'{2,5}", "", raw)
    raw = re.sub(r"^=+\s*(.*?)\s*=+$", r"\1", raw, flags=re.M)
    raw = re.sub(r"<br\s*/?>", "\n\n", raw, flags=re.I)
    raw = re.sub(r"</?(?:includeonly|onlyinclude|small|big|span|div)[^>]*>", "", raw)
    return html.unescape(raw).strip()

if __name__ == "__main__":
    rights = query(action="query", meta="siteinfo", siprop="rightsinfo")["query"]["rightsinfo"]
    assert rights["url"] == "https://www.gnu.org/copyleft/fdl.html", rights
    pages = query(action="query", list="allpages", apnamespace=10,
                  apprefix="היום יום/", aplimit=500)["query"]["allpages"]
    pages = [p for p in pages if re.match(r"תבנית:היום יום/[א-ת'\"]+ ", p["title"])]
    source = {}
    for start in range(0, len(pages), 40):
        batch = pages[start:start + 40]
        result = query(action="query", prop="revisions", rvprop="ids|timestamp|content",
                       pageids="|".join(str(p["pageid"]) for p in batch))
        for page in result["query"]["pages"].values():
            revision = page["revisions"][0]
            source[page["title"]] = dict(pageId=page["pageid"], revisionId=revision["revid"],
                                         timestamp=revision["timestamp"], wikitext=revision["*"])
    values = {"א":1,"ב":2,"ג":3,"ד":4,"ה":5,"ו":6,"ז":7,"ח":8,"ט":9,"י":10,"כ":20,"ל":30}
    months = {"תשרי":"tishrei", "חשוון":"cheshvan", "כסלו":"kislev", "טבת":"tevet",
              "שבט":"shevat", "אדר":"adar", "אדר א'":"adar1", "אדר ב'":"adar2",
              "ניסן":"nisan", "אייר":"iyar", "סיוון":"sivan", "תמוז":"tammuz",
              "מנחם אב":"av", "אלול":"elul"}
    entries = {}
    for title, revision in source.items():
        date = title.split("/", 1)[1]
        numeral, month = date.split(" ", 1)
        day = sum(values[c] for c in numeral if c in values)
        key = f"{months[month]}-{day}"
        raw = revision["wikitext"]
        # Common-year Adar pages may transclude leap-year days. Resolve only
        # exact Hayom Yom transclusions, never navigation or arbitrary templates.
        for _ in range(4):
            expanded = re.sub(r"\{\{(?:תבנית:)?(היום יום/[^{}]+)\}\}",
                lambda m: source["תבנית:" + m[1]]["wikitext"], raw)
            if expanded == raw: break
            raw = expanded
        text = clean(raw)
        if "{{" in text or "#הפניה" in text or "#REDIRECT" in text or "<" in text:
            raise ValueError(f"Unresolved source markup in {title}: {text[:250]}")
        assert text and 1 <= day <= 30, title
        entries[key] = dict(dateLabel=date, paragraphs=[p.strip() for p in re.split(r"\n\s*\n", text) if p.strip()],
                            sourceTitle=title, sourceUrl="https://text.chabadpedia.com/index.php?title=" + urllib.parse.quote(title),
                            revisionId=revision["revisionId"])
    assert len(entries) >= 380, len(entries)
    assets = ROOT / "app/src/main/assets"
    payload = dict(version="chabadtext-2026-10", title="היום יום", license="GFDL-1.3-or-later",
                   attribution="חב״דטקסט — תורמי דפי היום יום; מלקט: רבי מנחם מענדל שניאורסון",
                   modifications="הוסרו ניווט וסימוני עיצוב ויקי; תבניות תרגום מוצגות בסוגריים. המקור השקוף מצורף.", entries=entries)
    license_path = assets / "GFDL-1.3.txt"
    if not license_path.exists():
        with urllib.request.urlopen("https://www.gnu.org/licenses/fdl-1.3.txt", timeout=45) as response:
            license_bytes = response.read()
        assert len(license_bytes) > 20000 and b"Version 1.3" in license_bytes
        license_path.write_bytes(license_bytes)
    assert license_path.stat().st_size > 20000 and "Version 1.3" in license_path.read_text()
    (assets / "hayom_yom.json").write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n")
    (assets / "hayom_yom_source.json").write_text(json.dumps(source, ensure_ascii=False, indent=2) + "\n")
    print(f"Imported {len(entries)} Hebrew-date entries with transparent source and GFDL license")
