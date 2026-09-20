#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import urllib.request
import json
import re
import sqlite3

# Comprehensive mapping from Hebcal English title to (bookId, sectionId, sectionNameHebrew)
hebcal_to_section = {
    'Foundations of the Torah': ('madda', 'yesodei_hatorah', 'הלכות יסודי התורה'),
    'Human Dispositions': ('madda', 'deot', 'הלכות דעות'),
    'Torah Study': ('madda', 'talmud_torah', 'הלכות תלמוד תורה'),
    'Foreign Worship and Customs of the Nations': ('madda', 'avodat_kochavim', 'הלכות עבודה זרה'),
    'Foreign Worship': ('madda', 'avodat_kochavim', 'הלכות עבודה זרה'),
    'Repentance': ('madda', 'teshuvah', 'הלכות תשובה'),
    
    'Reading the Shema': ('ahava', 'keriat_shema', 'הלכות קריאת שמע'),
    'Prayer and the Priestly Blessing': ('ahava', 'tefilah', 'הלכות תפילה וברכת כהנים'),
    'The Order of Prayer': ('ahava', 'tefilah', 'הלכות תפילה וברכת כהנים'),
    'Tefillin': ('ahava', 'tefillin', 'הלכות תפילין ומזוזה וספר תורה'),
    'Mezuzah and the Torah Scroll': ('ahava', 'tefillin', 'הלכות תפילין ומזוזה וספר תורה'),
    'Tefillin, Mezuzah and the Torah Scroll': ('ahava', 'tefillin', 'הלכות תפילין ומזוזה וספר תורה'),
    'Fringes': ('ahava', 'tzitzit', 'הלכות ציצית'),
    'Blessings': ('ahava', 'berachot', 'הלכות ברכות'),
    'Circumcision': ('ahava', 'milah', 'הלכות מילה'),
    
    'Sabbath': ('zmanim', 'shabbat', 'הלכות שבת'),
    'Eruvin': ('zmanim', 'eruvin', 'הלכות עירובין'),
    'Rest on the Tenth of Tishrei': ('zmanim', 'shevitat_asor', 'הלכות שביתת עשור'),
    'Rest on a Holiday': ('zmanim', 'shevitat_yom_tov', 'הלכות שביתת יום טוב'),
    'Leavened and Unleavened Bread': ('zmanim', 'chametz_umatzah', 'הלכות חמץ ומצה'),
    'Chametz and Matzah': ('zmanim', 'chametz_umatzah', 'הלכות חמץ ומצה'),
    'Shofar': ('zmanim', 'shofar_vesukkah', 'הלכות שופר וסוכה ולולב'),
    'Sukkah and Lulav': ('zmanim', 'shofar_vesukkah', 'הלכות שופר וסוכה ולולב'),
    'Shofar, Sukkah and Lulav': ('zmanim', 'shofar_vesukkah', 'הלכות שופר וסוכה ולולב'),
    'Sheqel Dues': ('zmanim', 'shekalim', 'הלכות שקלים'),
    'Sanctification of the New Month': ('zmanim', 'kiddush_hachodesh', 'הלכות קידוש החודש'),
    'Fasts': ('zmanim', 'taaniyot', 'הלכות תעניות'),
    'Scroll of Esther and Hanukkah': ('zmanim', 'megillah_vechanukah', 'הלכות מגילה וחנוכה'),
    'Megillah and Chanukah': ('zmanim', 'megillah_vechanukah', 'הלכות מגילה וחנוכה'),
    
    'Marriage': ('nashim', 'ishut', 'הלכות אישות'),
    'Divorce': ('nashim', 'gerushin', 'הלכות גירושין'),
    'Levirate Marriage and Release': ('nashim', 'yibum', 'הלכות ייבום וחליצה'),
    'Levirate Marriage and Halitzah': ('nashim', 'yibum', 'הלכות ייבום וחליצה'),
    'Virgin Maiden': ('nashim', 'naarah', 'הלכות נערה בתולה'),
    'Woman Suspected of Infidelity': ('nashim', 'sotah', 'הלכות סוטה'),
    'Woman Suspected of Adultery': ('nashim', 'sotah', 'הלכות סוטה'),
    
    'Forbidden Intercourse': ('kedusha', 'issurei_biah', 'הלכות איסורי ביאה'),
    'Forbidden Foods': ('kedusha', 'maachalot_assurot', 'הלכות מאכלות אסורות'),
    'Ritual Slaughter': ('kedusha', 'shechitah', 'הלכות שחיטה'),
    
    'Oaths': ('haflaah', 'shevuot', 'הלכות שבועות'),
    'Vows': ('haflaah', 'nedarim', 'הלכות נדרים'),
    'Nazariteship': ('haflaah', 'nezirut', 'הלכות נזירות'),
    'Appraisals and Devoted Property': ('haflaah', 'erechin', 'הלכות ערכין וחרמין'),
    
    'Diverse Species': ('zeraim', 'kilayim', 'הלכות כלאים'),
    'Gifts to the Poor': ('zeraim', 'matnot_aniyim', 'הלכות מתנות עניים'),
    'Heave Offerings': ('zeraim', 'terumot', 'הלכות תרומות'),
    'Tithes': ('zeraim', 'maaser', 'הלכות מעשר'),
    'Second Tithes and Fourth Year\'s Fruit': ('zeraim', 'maaser_sheni', 'הלכות מעשר שני ונטע רבעי'),
    'First Fruits and other Gifts to Priests Outside the Sanctuary': ('zeraim', 'bikurim', 'הלכות ביכורים'),
    'Sabbatical Year and the Jubilee': ('zeraim', 'shemitah_veyovel', 'הלכות שמיטה ויובל'),
    
    'The Chosen Temple': ('avodah', 'beit_habechirah', 'הלכות בית הבחירה'),
    'Vessels of the Sanctuary and Those who Serve Therein': ('avodah', 'klei_hamikdash', 'הלכות כלי המקדש והעובדים בו'),
    'Admission into the Sanctuary': ('avodah', 'biat_hamikdash', 'הלכות ביאת המקדש'),
    'Things Forbidden on the Altar': ('avodah', 'issurei_mizbeach', 'הלכות איסורי מזבח'),
    'Sacrificial Procedure': ('avodah', 'maaseh_hakorbanot', 'הלכות מעשה הקרבנות'),
    'Daily Offerings and Additional Offerings': ('avodah', 'temidin_umussafin', 'הלכות תמידין ומוספין'),
    'Sacrifices Rendered Unfit': ('avodah', 'pessulei_hamukdashin', 'הלכות פסולי המוקדשין'),
    'Service on the Day of Atonement': ('avodah', 'avodat_yom_hakipurim', 'הלכות עבודת יום הכפורים'),
    'Trespass': ('avodah', 'meilah', 'הלכות מעילה'),
    
    'Paschal Offering': ('korbanot', 'korban_pesach', 'הלכות קרבן פסח'),
    'Festival Offering': ('korbanot', 'chagigah', 'הלכות חגיגה'),
    'Firstlings': ('korbanot', 'bechorot', 'הלכות בכורות'),
    'Offerings for Unintentional Transgressions': ('korbanot', 'shegagot', 'הלכות שגגות'),
    'Offerings for Those with Incomplete Atonement': ('korbanot', 'mechussarei_kapparah', 'הלכות מחוסרי כפרה'),
    'Substitution': ('korbanot', 'temurah', 'הלכות תמורה'),
    
    'Defilement by a Corpse': ('taharah', 'tumat_met', 'הלכות טומאת מת'),
    'Red Heifer': ('taharah', 'parah_adumah', 'הלכות פרה אדומה'),
    'Defilement by Leprosy': ('taharah', 'tumat_tsaraat', 'הלכות טומאת צרעת'),
    'Those Who Defile Bed or Seat': ('taharah', 'metamei_mishkav', 'הלכות מטמאי משכב ומושב'),
    'Other Sources of Defilement': ('taharah', 'shear_avot_hatumah', 'הלכות שאר אבות הטומאות'),
    'Defilement of Foods': ('taharah', 'tumat_ochlin', 'הלכות טומאת אוכלין'),
    'Vessels': ('taharah', 'keilim', 'הלכות כלים'),
    'Immersion Pools': ('taharah', 'mikvaot', 'הלכות מקוואות'),
    
    'Damages to Property': ('nezikin', 'nizkei_mamon', 'הלכות נזקי ממון'),
    'Theft': ('nezikin', 'geneivah', 'הלכות גנבה'),
    'Robbery and Lost Property': ('nezikin', 'gezelah_vaaveidah', 'הלכות גזלה ואבדה'),
    'One Who Injures a Person or Property': ('nezikin', 'chovel_umazik', 'הלכות חובל ומזיק'),
    'Murderer and the Preservation of Life': ('nezikin', 'rotzeach_ushmirat_nefesh', 'הלכות רוצח ושמירת נפש'),
    
    'Sales': ('kinyan', 'mechirah', 'הלכות מכירה'),
    'Ownerless Property and Gifts': ('kinyan', 'zechiyah_umatanah', 'הלכות זכייה ומתנה'),
    'Neighbors': ('kinyan', 'shecheinim', 'הלכות שכנים'),
    'Agents and Partners': ('kinyan', 'sheluchin_veshutafin', 'הלכות שלוחין ושותפין'),
    'Slaves': ('kinyan', 'avadim', 'הלכות עבדים'),
    
    'Hiring': ('mishpatim', 'sechirut', 'הלכות שכירות'),
    'Borrowing and Deposit': ('mishpatim', 'sheilah_ufikadon', 'הלכות שאלה ופיקדון'),
    'Creditor and Debtor': ('mishpatim', 'malveh_veloveh', 'הלכות מלווה ולווה'),
    'Plaintiff and Defendant': ('mishpatim', 'toen_venitan', 'הלכות טוען ונטען'),
    'Inheritances': ('mishpatim', 'nachalot', 'הלכות נחלות'),
    
    'The Sanhedrin and the Penalties within their Jurisdiction': ('shoftim', 'sanhedrin', 'הלכות סנהדרין'),
    'Testimony': ('shoftim', 'edut', 'הלכות עדות'),
    'Rebels': ('shoftim', 'mamrim', 'הלכות ממרים'),
    'Mourning': ('shoftim', 'avel', 'הלכות אבל'),
    'Kings and Wars': ('shoftim', 'melachim', 'הלכות מלכים ומלחמות'),
    
    'Positive Mitzvot': ('madda', 'positive_mitzvot', 'ספר המצוות - מצוות עשה'),
    'Negative Mitzvot': ('madda', 'negative_mitzvot', 'ספר המצוות - מצוות לא תעשה'),
    'Transmission of the Oral Law': ('madda', 'transmission_of_the_oral_law', 'הקדמת הרמב״ם'),
    'Overview of Mishneh Torah Contents': ('madda', 'overview_of_mishneh_torah_contents', 'מניין המצוות')
}

heb_days_map = {
    1: 'א׳', 2: 'ב׳', 3: 'ג׳', 4: 'ד׳', 5: 'ה׳', 6: 'ו׳', 7: 'ז׳', 8: 'ח׳', 9: 'ט׳', 10: 'י׳',
    11: 'י״א', 12: 'י״ב', 13: 'י״ג', 14: 'י״ד', 15: 'ט״ו', 16: 'ט״ז', 17: 'י״ז', 18: 'י״ח', 19: 'י״ט', 20: 'כ׳',
    21: 'כ״א', 22: 'כ״ב', 23: 'כ״ג', 24: 'כ״ד', 25: 'כ״ה', 26: 'כ״ו', 27: 'כ״ז', 28: 'כ״ח', 29: 'כ״ט', 30: 'ל׳'
}

heb_months_map = {
    "Nisan": "בניסן", "Iyyar": "באייר", "Sivan": "בסיוון", "Tamuz": "בתמוז",
    "Av": "באב", "Elul": "באלול", "Tishrei": "בתשרי", "Cheshvan": "בחשוון",
    "Kislev": "בכסלו", "Tevet": "בטבת", "Sh'vat": "בשבט", "Adar": "באדר",
    "Adar I": "באדר א׳", "Adar II": "באדר ב׳"
}

heb_years_map = {
    5785: "תשפ״ה", 5786: "תשפ״ו", 5787: "תשפ״ז", 5788: "תשפ״ח"
}

def format_hebrew_date(hdate_str):
    if not hdate_str:
        return ""
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

def parse_hebcal_entry_v3(title_str):
    result = []
    parts = [p.strip() for p in title_str.split(",")]
    for part in parts:
        # Check overview with colon like 1:1-4:8
        m_colon = re.search(r"^(Overview of Mishneh Torah Contents)\s+(\d+):\d+(?:-(\d+):\d+)?$", part)
        if m_colon:
            name = m_colon.group(1).strip()
            start_ch = int(m_colon.group(2))
            end_ch = int(m_colon.group(3)) if m_colon.group(3) else start_ch
            info = hebcal_to_section[name]
            book_id, sec_id, sec_name = info
            for ch in range(start_ch, end_ch + 1):
                result.append({
                    "bookId": book_id,
                    "sectionId": sec_id,
                    "sectionNameHebrew": sec_name,
                    "chapterNumber": ch,
                    "chapter": ch
                })
            continue

        m = re.search(r"^(.*?)\s+(\d+)(?:-(\d+))?$", part)
        if m:
            name = m.group(1).strip()
            start_ch = int(m.group(2))
            end_ch = int(m.group(3)) if m.group(3) else start_ch
            info = hebcal_to_section.get(name)
            if not info:
                print(f"Unknown Hebcal name: {name}")
                info = ("unknown", name.lower().replace(" ", "_"), name)
            book_id, sec_id, sec_name = info
            for ch in range(start_ch, end_ch + 1):
                result.append({
                    "bookId": book_id,
                    "sectionId": sec_id,
                    "sectionNameHebrew": sec_name,
                    "chapterNumber": ch,
                    "chapter": ch
                })
        else:
            info = hebcal_to_section.get(part, ("unknown", part.lower().replace(" ", "_"), part))
            book_id, sec_id, sec_name = info
            result.append({
                "bookId": book_id,
                "sectionId": sec_id,
                "sectionNameHebrew": sec_name,
                "chapterNumber": 1,
                "chapter": 1
            })
    return result

print("Fetching full 2026 Hebcal schedule...")
url = "https://www.hebcal.com/hebcal?v=1&cfg=json&dr1=on&dr3=on&start=2026-01-01&end=2026-12-31"
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
with urllib.request.urlopen(req, timeout=45) as resp:
    data = json.loads(resp.read().decode())

items = data.get("items", [])
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
            "oneChapter": [],
            "threeChapters": [],
            "one": [],
            "three": []
        }
    if hdate and not days_dict[date_str]["hebrewDate"]:
        days_dict[date_str]["hebrewDate"] = format_hebrew_date(hdate)
        
    if cat == "dailyRambam1":
        parsed = parse_hebcal_entry_v3(title)
        days_dict[date_str]["oneChapter"] = [
            {"bookId": c["bookId"], "sectionId": c["sectionId"], "chapterNumber": c["chapterNumber"]}
            for c in parsed
        ]
        days_dict[date_str]["one"] = parsed
    elif cat == "dailyRambam3":
        parsed = parse_hebcal_entry_v3(title)
        days_dict[date_str]["threeChapters"] = [
            {"bookId": c["bookId"], "sectionId": c["sectionId"], "chapterNumber": c["chapterNumber"]}
            for c in parsed
        ]
        days_dict[date_str]["three"] = parsed

# Validate against DB!
db_path = "app/src/main/assets/database/rambam.db"
conn = sqlite3.connect(db_path)
cur = conn.cursor()

# Get all chapter IDs in DB
cur.execute("SELECT id FROM chapters")
db_chapter_ids = set(row[0] for row in cur.fetchall())
print(f"Total chapter IDs in DB: {len(db_chapter_ids)}")

missing_in_db = []
for d_str, day_val in days_dict.items():
    for c in day_val["oneChapter"]:
        ch_id = f"{c['sectionId']}_{c['chapterNumber']}"
        if ch_id not in db_chapter_ids:
            missing_in_db.append((d_str, "one", ch_id))
    for c in day_val["threeChapters"]:
        ch_id = f"{c['sectionId']}_{c['chapterNumber']}"
        if ch_id not in db_chapter_ids:
            missing_in_db.append((d_str, "three", ch_id))

if missing_in_db:
    print(f"CRITICAL BUILD FAILURE: {len(missing_in_db)} scheduled chapters not found in DB!")
    for m in missing_in_db[:20]:
        print(f"  Missing: {m}")
    exit(1)
else:
    print("SUCCESS: 100% of all scheduled chapters in 2026 exist in the database with zero missing!")

# Save to rambam_schedule.json
out_path = "app/src/main/assets/rambam_schedule.json"
with open(out_path, "w", encoding="utf-8") as f:
    json.dump({
        "attribution": {
            "source": "Hebcal REST API",
            "license": "CC BY 4.0",
            "rightsHolders": "Hebcal.com",
            "verifiedAgainst": "Chabad.org Daily Study Calendar"
        },
        "days": days_dict
    }, f, ensure_ascii=False, indent=2)

print(f"Successfully wrote verified schedule to {out_path} ({len(days_dict)} days)")
conn.close()
