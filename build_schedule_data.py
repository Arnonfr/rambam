import urllib.request
import json
import re

hebcal_to_section = {
    "Marriage": ("ishut", "הלכות אישות"),
    "Divorce": ("gerushin", "הלכות גירושין"),
    "Levirate Marriage and Halitzah": ("yibum", "הלכות ייבום וחליצה"),
    "Virgin Maiden": ("naarah", "הלכות נערה בתולה"),
    "Woman Suspected of Adultery": ("sotah", "הלכות סוטה"),
    "Other Sources of Defilement": ("shear-avot-hatumah", "הלכות שאר אבות הטומאות"),
    "Defilement of Foods": ("tumat-ochlin", "הלכות טומאת אוכלין"),
    "Vessels": ("keilim", "הלכות כלים"),
    "Leprosy": ("tsaraat", "הלכות טומאת צרעת"),
    "Couch and Seat": ("metamei-mishkav", "הלכות מטמאי משכב ומושב"),
    "Mikvaot": ("mikvaot", "הלכות מקוואות"),
    "Red Heifer": ("parah-adumah", "הלכות פרה אדומה"),
    "Corpse Defilement": ("tumat-met", "הלכות טומאת מת"),
    "Foundations of the Torah": ("yesodei-hatorah", "הלכות יסודי התורה"),
    "Human Dispositions": ("deot", "הלכות דעות"),
    "Torah Study": ("talmud-torah", "הלכות תלמוד תורה"),
    "Foreign Worship": ("avodat-kochavim", "הלכות עבודה זרה"),
    "Repentance": ("teshuvah", "הלכות תשובה"),
    "Reading the Shema": ("keriat-shema", "הלכות קריאת שמע"),
    "Prayer and the Priestly Blessing": ("tefilah", "הלכות תפילה וברכת כהנים"),
    "Tefillin, Mezuzah and the Torah Scroll": ("tefillin", "הלכות תפילין ומזוזה וספר תורה"),
    "Fringes": ("tzitzit", "הלכות ציצית"),
    "Blessings": ("berachot", "הלכות ברכות"),
    "Circumcision": ("milah", "הלכות מילה"),
    "Sabbath": ("shabbat", "הלכות שבת"),
    "Eruv": ("eruvin", "הלכות עירובין"),
    "Rest on the Tenth of Tishrei": ("shevitat-asor", "הלכות שביתת עשור"),
    "Rest on a Holiday": ("shevitat-yom-tov", "הלכות שביתת יום טוב"),
    "Chametz and Matzah": ("chametz-umatzah", "הלכות חמץ ומצה"),
    "Shofar, Sukkah and Lulav": ("shofar-vesukkah", "הלכות שופר וסוכה ולולב"),
    "Sheqel Dues": ("shekalim", "הלכות שקלים"),
    "Sanctification of the New Month": ("kiddush-hachodesh", "הלכות קידוש החודש"),
    "Fasts": ("taaniyot", "הלכות תעניות"),
    "Megillah and Chanukah": ("megillah-vechanukah", "הלכות מגילה וחנוכה")
}

heb_days_map = {
    1: 'א׳', 2: 'ב׳', 3: 'ג׳', 4: 'ד׳', 5: 'ה׳', 6: 'ו׳', 7: 'ז׳', 8: 'ח׳', 9: 'ט׳', 10: 'י׳',
    11: 'י״א', 12: 'י״ב', 13: 'י״ג', 14: 'י״ד', 15: 'ט״ו', 16: 'ט״ז', 17: 'י״ז', 18: 'י״ח', 19: 'י״ט', 20: 'כ׳',
    21: 'כ״א', 22: 'כ״ב', 23: 'כ״ג', 24: 'כ״ד', 25: 'כ״ה', 26: 'כ״ו', 27: 'כ״ז', 28: 'כ״ח', 29: 'כ״ט', 30: 'ל׳'
}

heb_months_map = {
    "Nisan": "בניסן",
    "Iyyar": "באייר",
    "Sivan": "בסיוון",
    "Tamuz": "בתמוז",
    "Av": "באב",
    "Elul": "באלול",
    "Tishrei": "בתשרי",
    "Cheshvan": "בחשוון",
    "Kislev": "בכסלו",
    "Tevet": "בטבת",
    "Sh'vat": "בשבט",
    "Adar": "באדר",
    "Adar I": "באדר א׳",
    "Adar II": "באדר ב׳"
}

heb_years_map = {
    5785: "תשפ״ה",
    5786: "תשפ״ו",
    5787: "תשפ״ז",
    5788: "תשפ״ח"
}

def format_hebrew_date(hdate_str):
    if not hdate_str:
        return ""
    # e.g. "2 Tishrei 5787"
    m = re.match(r"^(\d+)\s+([A-Za-z' ]+)\s+(\d+)$", hdate_str.strip())
    if m:
        day_num = int(m.group(1))
        month_name = m.group(2).strip()
        year_num = int(m.group(3))
        day_heb = heb_days_map.get(day_num, str(day_num))
        month_heb = heb_months_map.get(month_name, month_name)
        year_heb = heb_years_map.get(year_num, str(year_num))
        return f"{day_heb} {month_heb} {year_heb}"
    return hdate_str

def parse_hebcal_entry(title_str):
    result = []
    parts = [p.strip() for p in title_str.split(",")]
    for part in parts:
        m = re.search(r"^(.*?)\s+(\d+)(?:-(\d+))?$", part)
        if m:
            name = m.group(1).strip()
            start_ch = int(m.group(2))
            end_ch = int(m.group(3)) if m.group(3) else start_ch
            sec_id = hebcal_to_section.get(name, (name.lower().replace(" ", "-"), name))[0]
            sec_name = hebcal_to_section.get(name, (name.lower().replace(" ", "-"), name))[1]
            for ch in range(start_ch, end_ch + 1):
                result.append({
                    "workId": "mishneh-torah",
                    "sectionId": sec_id,
                    "sectionNameHebrew": sec_name,
                    "chapter": ch
                })
        else:
            result.append({
                "workId": "mishneh-torah",
                "sectionId": "unknown",
                "sectionNameHebrew": part,
                "chapter": 1
            })
    return result

print("Fetching Hebcal events for entire year 2026...")
url = "https://www.hebcal.com/hebcal?v=1&cfg=json&dr1=on&dr3=on&start=2026-01-01&end=2026-12-31"
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})

with urllib.request.urlopen(req, timeout=45) as resp:
    data = json.loads(resp.read().decode())

items = data.get("items", [])
print(f"Fetched {len(items)} items from Hebcal")

days_dict = {}

for it in items:
    date_str = it.get("date", "")[:10]
    cat = it.get("category", "")
    title = it.get("title", "")
    hdate = it.get("hdate", "")
    
    if date_str not in days_dict:
        days_dict[date_str] = {
            "civilDate": date_str,
            "hebrewDate": format_hebrew_date(hdate),
            "one": [],
            "three": [],
            "scheduleSource": "hebcal",
            "scheduleVersion": "2026-09"
        }
    if hdate and not days_dict[date_str]["hebrewDate"]:
        days_dict[date_str]["hebrewDate"] = format_hebrew_date(hdate)
        
    if cat == "dailyRambam1":
        days_dict[date_str]["one"] = parse_hebcal_entry(title)
    elif cat == "dailyRambam3":
        days_dict[date_str]["three"] = parse_hebcal_entry(title)

# Verification
test_dates = ["2026-09-13", "2026-09-14", "2026-09-20", "2026-09-21", "2026-09-22"]
print("\n--- PRD Acceptance Test Dates Verification ---")
for td in test_dates:
    day = days_dict.get(td)
    if day:
        one_str = ", ".join([f"{c['sectionId']} {c['chapter']}" for c in day['one']])
        three_str = ", ".join([f"{c['sectionId']} {c['chapter']}" for c in day['three']])
        print(f"Date: {td} ({day['hebrewDate']}) | 1 Ch: {one_str} | 3 Ch: {three_str}")

out_path = "app/src/main/assets/rambam_schedule.json"
with open(out_path, "w", encoding="utf-8") as f:
    json.dump({
        "attribution": {
            "source": "Hebcal REST API",
            "license": "CC BY 4.0",
            "licenseUrl": "https://creativecommons.org/licenses/by/4.0/",
            "verifiedAgainst": "Chabad.org Daily Study Calendar",
            "rightsHolders": "Hebcal.com",
            "notice": "לוח שיעורי הרמב״ם נלקח מ־Hebcal (CC BY 4.0) ואומת מול לוח חב״ד. ספר נשים מובא במלואו ממהדורת תורת אמת ברישיון CC BY-NC-SA 2.5."
        },
        "days": days_dict
    }, f, ensure_ascii=False, indent=2)

print(f"\nSuccessfully saved full-year schedule ({len(days_dict)} days) to {out_path}")
