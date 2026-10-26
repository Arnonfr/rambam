"""Cache Israel/diaspora weekly parasha IDs; boundaries come only from XLSX.

Holiday study rules are applied by ChumashRepository, not by copying public
festival readings into the daily learning schedule.
"""
import json
import re
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent
CACHE = ROOT.parent / "מחקר/chumash-daily"
START, END = "2025-01-01", "2031-12-31"


def normalize(name):
    name = re.sub(r'[\s\-־–"״\u0591-\u05c7]', '', name)
    for source, target in [("בחוקותי", "בחוקתי"), ("חוקת", "חקת"), ("מצרע", "מצורע"),
                           ("שלחלך", "שלח"), ("אחרימות", "אחרי"), ("קדשים", "קדושים"),
                           ("קורח", "קרח"), ("בהעלתך", "בהעלותך"), ("נוח", "נח")]:
        name = name.replace(source, target)
    return name


def main():
    parashot = json.loads((ROOT / "app/src/main/assets/chumash_cache.json").read_text())
    ids = {normalize(name): obj["parashaId"] for name, obj in parashot.items()}
    output = {"schemaVersion": 1, "start": START, "end": END,
              "source": "https://www.hebcal.com/home/195/jewish-calendar-rest-api", "schedules": {}}
    CACHE.mkdir(parents=True, exist_ok=True)
    for in_israel in (True, False):
        key = "israel" if in_israel else "diaspora"
        path = CACHE / f"hebcal-{key}.json"
        if path.exists():
            data = json.loads(path.read_text())
        else:
            url = (f"https://www.hebcal.com/hebcal?v=1&cfg=json&start={START}&end={END}"
                   f"&s=on&i={'on' if in_israel else 'off'}&leyning=off")
            with urllib.request.urlopen(url, timeout=30) as response:
                data = json.load(response)
            path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n")
        output["schedules"][key] = [{"date": row["date"],
            "parashaId": ids[normalize(row["hebrew"].removeprefix("פרשת "))]}
            for row in data["items"] if row["category"] == "parashat"]
    (ROOT / "app/src/main/assets/chumash_weekly_schedule.json").write_text(
        json.dumps(output, ensure_ascii=False, indent=2) + "\n")
    print({key: len(rows) for key, rows in output["schedules"].items()})


if __name__ == "__main__":
    main()
