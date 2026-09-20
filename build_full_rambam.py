#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import urllib.request
import re
import html
import json
import os
import sqlite3

# 14 books of Mishneh Torah in Torat Emet
BOOKS_CONFIG = [
    {
        "bookId": "madda",
        "toratEmetNum": 1997,
        "titleHebrew": "ספר מדע",
        "sections": [
            {"sectionId": "yesodei_hatorah", "titleHebrew": "הלכות יסודי התורה", "numChapters": 10, "prefix": "הלכותBיסודיBהתורה"},
            {"sectionId": "deot", "titleHebrew": "הלכות דעות", "numChapters": 7, "prefix": "הלכותBדעות"},
            {"sectionId": "talmud_torah", "titleHebrew": "הלכות תלמוד תורה", "numChapters": 7, "prefix": "הלכותBתלמודBתורה"},
            {"sectionId": "avodat_kochavim", "titleHebrew": "הלכות עבודה זרה", "numChapters": 12, "prefix": "הלכותBעבודתBכוכביםBוחקותיהם"},
            {"sectionId": "teshuvah", "titleHebrew": "הלכות תשובה", "numChapters": 10, "prefix": "הלכותBתשובה"}
        ]
    },
    {
        "bookId": "ahava",
        "toratEmetNum": 1999,
        "titleHebrew": "ספר אהבה",
        "sections": [
            {"sectionId": "keriat_shema", "titleHebrew": "הלכות קריאת שמע", "numChapters": 4, "prefix": "הלכותBקריאתBשמע"},
            {"sectionId": "tefilah", "titleHebrew": "הלכות תפילה וברכת כהנים", "numChapters": 15, "prefix": "הלכותBתפילהBוברכתBכהנים"},
            {"sectionId": "tefillin", "titleHebrew": "הלכות תפילין ומזוזה וספר תורה", "numChapters": 10, "prefix": "הלכותBתפיליןBומזוזהBוספרBתורה"},
            {"sectionId": "tzitzit", "titleHebrew": "הלכות ציצית", "numChapters": 3, "prefix": "הלכותBציצית"},
            {"sectionId": "berachot", "titleHebrew": "הלכות ברכות", "numChapters": 11, "prefix": "הלכותBברכות"},
            {"sectionId": "milah", "titleHebrew": "הלכות מילה", "numChapters": 3, "prefix": "הלכותBמילה"}
        ]
    },
    {
        "bookId": "zmanim",
        "toratEmetNum": 2013,
        "titleHebrew": "ספר זמנים",
        "sections": [
            {"sectionId": "shabbat", "titleHebrew": "הלכות שבת", "numChapters": 30, "prefix": "הלכותBשבת"},
            {"sectionId": "eruvin", "titleHebrew": "הלכות עירובין", "numChapters": 8, "prefix": "הלכותBעירובין"},
            {"sectionId": "shevitat_asor", "titleHebrew": "הלכות שביתת עשור", "numChapters": 3, "prefix": "הלכותBשביתתBעשור"},
            {"sectionId": "shevitat_yom_tov", "titleHebrew": "הלכות שביתת יום טוב", "numChapters": 8, "prefix": "הלכותBשביתתBיוםBטוב"},
            {"sectionId": "chametz_umatzah", "titleHebrew": "הלכות חמץ ומצה", "numChapters": 8, "prefix": "הלכותBחמץBומצה"},
            {"sectionId": "shofar_vesukkah", "titleHebrew": "הלכות שופר וסוכה ולולב", "numChapters": 8, "prefix": "הלכותBשופרBוסוכהBולולב"},
            {"sectionId": "shekalim", "titleHebrew": "הלכות שקלים", "numChapters": 4, "prefix": "הלכותBשקלים"},
            {"sectionId": "kiddush_hachodesh", "titleHebrew": "הלכות קידוש החודש", "numChapters": 19, "prefix": "הלכותBקידושBהחודש"},
            {"sectionId": "taaniyot", "titleHebrew": "הלכות תעניות", "numChapters": 5, "prefix": "הלכותBתעניות"},
            {"sectionId": "megillah_vechanukah", "titleHebrew": "הלכות מגילה וחנוכה", "numChapters": 4, "prefix": "הלכותBמגילהBוחנוכה"}
        ]
    },
    {
        "bookId": "nashim",
        "toratEmetNum": 2017,
        "titleHebrew": "ספר נשים",
        "sections": [
            {"sectionId": "ishut", "titleHebrew": "הלכות אישות", "numChapters": 25, "prefix": "הלכותBאישות"},
            {"sectionId": "gerushin", "titleHebrew": "הלכות גירושין", "numChapters": 13, "prefix": "הלכותBגרושין"},
            {"sectionId": "yibum", "titleHebrew": "הלכות ייבום וחליצה", "numChapters": 8, "prefix": "הלכותBיבוםBוחליצה"},
            {"sectionId": "naarah", "titleHebrew": "הלכות נערה בתולה", "numChapters": 3, "prefix": "הלכותBנערהBבתולה"},
            {"sectionId": "sotah", "titleHebrew": "הלכות סוטה", "numChapters": 4, "prefix": "הלכותBסוטה"}
        ]
    },
    {
        "bookId": "kedusha",
        "toratEmetNum": 2009,
        "titleHebrew": "ספר קדושה",
        "sections": [
            {"sectionId": "issurei_biah", "titleHebrew": "הלכות איסורי ביאה", "numChapters": 22, "prefix": "הלכותBאיסוריBביאה"},
            {"sectionId": "maachalot_assurot", "titleHebrew": "הלכות מאכלות אסורות", "numChapters": 17, "prefix": "הלכותBמאכלותBאסורות"},
            {"sectionId": "shechitah", "titleHebrew": "הלכות שחיטה", "numChapters": 14, "prefix": "הלכותBשחיטה"}
        ]
    },
    {
        "bookId": "haflaah",
        "toratEmetNum": 2021,
        "titleHebrew": "ספר הפלאה",
        "sections": [
            {"sectionId": "shevuot", "titleHebrew": "הלכות שבועות", "numChapters": 12, "prefix": "הלכותBשבועות"},
            {"sectionId": "nedarim", "titleHebrew": "הלכות נדרים", "numChapters": 13, "prefix": "הלכותBנדרים"},
            {"sectionId": "nezirut", "titleHebrew": "הלכות נזירות", "numChapters": 10, "prefix": "הלכותBנזירות"},
            {"sectionId": "erechin", "titleHebrew": "הלכות ערכין וחרמין", "numChapters": 8, "prefix": "הלכותBערכיןBוחרמין"}
        ]
    },
    {
        "bookId": "zeraim",
        "toratEmetNum": 2022,
        "titleHebrew": "ספר זרעים",
        "sections": [
            {"sectionId": "kilayim", "titleHebrew": "הלכות כלאים", "numChapters": 10, "prefix": "הלכותBכלאים"},
            {"sectionId": "matnot_aniyim", "titleHebrew": "הלכות מתנות עניים", "numChapters": 10, "prefix": "הלכותBמתנותBעניים"},
            {"sectionId": "terumot", "titleHebrew": "הלכות תרומות", "numChapters": 15, "prefix": "הלכותBתרומות"},
            {"sectionId": "maaser", "titleHebrew": "הלכות מעשר", "numChapters": 14, "prefix": "הלכותBמעשר"},
            {"sectionId": "maaser_sheni", "titleHebrew": "הלכות מעשר שני ונטע רבעי", "numChapters": 11, "prefix": "הלכותBמעשרBשניBונטעBרבעי"},
            {"sectionId": "bikurim", "titleHebrew": "הלכות ביכורים", "numChapters": 12, "prefix": "הלכותBביכוריםBעםBשארBמתנותBכהונהBשבגבולין"},
            {"sectionId": "shemitah_veyovel", "titleHebrew": "הלכות שמיטה ויובל", "numChapters": 13, "prefix": "הלכותBשמיטהBויובל"}
        ]
    },
    {
        "bookId": "avodah",
        "toratEmetNum": 2001,
        "titleHebrew": "ספר עבודה",
        "sections": [
            {"sectionId": "beit_habechirah", "titleHebrew": "הלכות בית הבחירה", "numChapters": 8, "prefix": "הלכותBביתBהבחירה"},
            {"sectionId": "klei_hamikdash", "titleHebrew": "הלכות כלי המקדש והעובדים בו", "numChapters": 10, "prefix": "הלכותBכליBהמקדשBוהעובדיםBבו"},
            {"sectionId": "biat_hamikdash", "titleHebrew": "הלכות ביאת המקדש", "numChapters": 9, "prefix": "הלכותBביאתBהמקדש"},
            {"sectionId": "issurei_mizbeach", "titleHebrew": "הלכות איסורי מזבח", "numChapters": 7, "prefix": "הלכותBאיסוריBמזבח"},
            {"sectionId": "maaseh_hakorbanot", "titleHebrew": "הלכות מעשה הקרבנות", "numChapters": 19, "prefix": "הלכותBמעשהBהקרבנות"},
            {"sectionId": "temidin_umussafin", "titleHebrew": "הלכות תמידין ומוספין", "numChapters": 10, "prefix": "הלכותBתמידיןBומוספין"},
            {"sectionId": "pessulei_hamukdashin", "titleHebrew": "הלכות פסולי המוקדשין", "numChapters": 19, "prefix": "הלכותBפסוליBהמוקדשין"},
            {"sectionId": "avodat_yom_hakipurim", "titleHebrew": "הלכות עבודת יום הכפורים", "numChapters": 5, "prefix": "הלכותBעבודתBיוםBהכיפורים"},
            {"sectionId": "meilah", "titleHebrew": "הלכות מעילה", "numChapters": 8, "prefix": "הלכותBמעילה"}
        ]
    },
    {
        "bookId": "korbanot",
        "toratEmetNum": 2002,
        "titleHebrew": "ספר קרבנות",
        "sections": [
            {"sectionId": "korban_pesach", "titleHebrew": "הלכות קרבן פסח", "numChapters": 10, "prefix": "הלכותBקרבןBפסח"},
            {"sectionId": "chagigah", "titleHebrew": "הלכות חגיגה", "numChapters": 3, "prefix": "הלכותBחגיגה"},
            {"sectionId": "bechorot", "titleHebrew": "הלכות בכורות", "numChapters": 8, "prefix": "הלכותBבכורות"},
            {"sectionId": "shegagot", "titleHebrew": "הלכות שגגות", "numChapters": 15, "prefix": "הלכותBשגגות"},
            {"sectionId": "mechussarei_kapparah", "titleHebrew": "הלכות מחוסרי כפרה", "numChapters": 5, "prefix": "הלכותBמחוסריBכפרה"},
            {"sectionId": "temurah", "titleHebrew": "הלכות תמורה", "numChapters": 4, "prefix": "הלכותBתמורה"}
        ]
    },
    {
        "bookId": "taharah",
        "toratEmetNum": 2029,
        "titleHebrew": "ספר טהרה",
        "sections": [
            {"sectionId": "tumat_met", "titleHebrew": "הלכות טומאת מת", "numChapters": 25, "prefix": "הלכותBטומאתBמת"},
            {"sectionId": "parah_adumah", "titleHebrew": "הלכות פרה אדומה", "numChapters": 15, "prefix": "הלכותBפרהBאדומה"},
            {"sectionId": "tumat_tsaraat", "titleHebrew": "הלכות טומאת צרעת", "numChapters": 16, "prefix": "הלכותBטומאתBצרעת"},
            {"sectionId": "metamei_mishkav", "titleHebrew": "הלכות מטמאי משכב ומושב", "numChapters": 13, "prefix": "הלכותBמטמאיBמשכבBומושב"},
            {"sectionId": "shear_avot_hatumah", "titleHebrew": "הלכות שאר אבות הטומאות", "numChapters": 20, "prefix": "הלכותBשארBאבותBהטומאות"},
            {"sectionId": "tumat_ochlin", "titleHebrew": "הלכות טומאת אוכלין", "numChapters": 16, "prefix": "הלכותBטומאתBאוכלין"},
            {"sectionId": "keilim", "titleHebrew": "הלכות כלים", "numChapters": 28, "prefix": "הלכותBכלים"},
            {"sectionId": "mikvaot", "titleHebrew": "הלכות מקוואות", "numChapters": 11, "prefix": "הלכותBמקואות"}
        ]
    },
    {
        "bookId": "nezikin",
        "toratEmetNum": 2035,
        "titleHebrew": "ספר נזיקין",
        "sections": [
            {"sectionId": "nizkei_mamon", "titleHebrew": "הלכות נזקי ממון", "numChapters": 14, "prefix": "הלכותBנזקיBממון"},
            {"sectionId": "geneivah", "titleHebrew": "הלכות גנבה", "numChapters": 9, "prefix": "הלכותBגנבה"},
            {"sectionId": "gezelah_vaaveidah", "titleHebrew": "הלכות גזלה ואבדה", "numChapters": 18, "prefix": "הלכותBגזלהBואבדה"},
            {"sectionId": "chovel_umazik", "titleHebrew": "הלכות חובל ומזיק", "numChapters": 8, "prefix": "הלכותBחובלBומזיק"},
            {"sectionId": "rotzeach_ushmirat_nefesh", "titleHebrew": "הלכות רוצח ושמירת נפש", "numChapters": 13, "prefix": "הלכותBרוצחBושמירתBנפש"}
        ]
    },
    {
        "bookId": "kinyan",
        "toratEmetNum": 2036,
        "titleHebrew": "ספר קניין",
        "sections": [
            {"sectionId": "mechirah", "titleHebrew": "הלכות מכירה", "numChapters": 30, "prefix": "הלכותBמכירה"},
            {"sectionId": "zechiyah_umatanah", "titleHebrew": "הלכות זכייה ומתנה", "numChapters": 12, "prefix": "הלכותBזכיהBומתנה"},
            {"sectionId": "shecheinim", "titleHebrew": "הלכות שכנים", "numChapters": 14, "prefix": "הלכותBשכנים"},
            {"sectionId": "sheluchin_veshutafin", "titleHebrew": "הלכות שלוחין ושותפין", "numChapters": 10, "prefix": "הלכותBשלוחיןBושותפין"},
            {"sectionId": "avadim", "titleHebrew": "הלכות עבדים", "numChapters": 9, "prefix": "הלכותBעבדים"}
        ]
    },
    {
        "bookId": "mishpatim",
        "toratEmetNum": 2037,
        "titleHebrew": "ספר משפטים",
        "sections": [
            {"sectionId": "sechirut", "titleHebrew": "הלכות שכירות", "numChapters": 13, "prefix": "הלכותBשכירות"},
            {"sectionId": "sheilah_ufikadon", "titleHebrew": "הלכות שאלה ופיקדון", "numChapters": 8, "prefix": "הלכותBשאלהBופקדון"},
            {"sectionId": "malveh_veloveh", "titleHebrew": "הלכות מלווה ולווה", "numChapters": 27, "prefix": "הלכותBמלוהBולוה"},
            {"sectionId": "toen_venitan", "titleHebrew": "הלכות טוען ונטען", "numChapters": 16, "prefix": "הלכותBטועןBונטען"},
            {"sectionId": "nachalot", "titleHebrew": "הלכות נחלות", "numChapters": 11, "prefix": "הלכותBנחלות"}
        ]
    },
    {
        "bookId": "shoftim",
        "toratEmetNum": 2038,
        "titleHebrew": "ספר שופטים",
        "sections": [
            {"sectionId": "sanhedrin", "titleHebrew": "הלכות סנהדרין", "numChapters": 26, "prefix": "הלכותBסנהדריןBוהעונשיןBהמסוריםBלהם"},
            {"sectionId": "edut", "titleHebrew": "הלכות עדות", "numChapters": 22, "prefix": "הלכותBעדות"},
            {"sectionId": "mamrim", "titleHebrew": "הלכות ממרים", "numChapters": 7, "prefix": "הלכותBממרים"},
            {"sectionId": "avel", "titleHebrew": "הלכות אבל", "numChapters": 14, "prefix": "הלכותBאבל"},
            {"sectionId": "melachim", "titleHebrew": "הלכות מלכים ומלחמות", "numChapters": 12, "prefix": "הלכותBמלכיםBומלחמותיהם"}
        ]
    }
]

heb_ordinals = [
    '', 'ראשון', 'שני', 'שלישי', 'רביעי', 'חמישי', 'ששי', 'שביעי', 'שמיני', 'תשיעי', 'עשירי',
    'אחדBעשר', 'שניםBעשר', 'שלשהBעשר', 'ארבעהBעשר', 'חמשהBעשר', 'ששהBעשר', 'שבעהBעשר', 'שמונהBעשר', 'תשעהBעשר', 'עשרים',
    'אחדBועשרים', 'שניםBועשרים', 'שלשהBועשרים', 'ארבעהBועשרים', 'חמשהBועשרים', 'ששהBועשרים', 'שבעהBועשרים', 'שמונהBועשרים', 'תשעהBועשרים', 'שלשים'
]

hebrew_numeral_map = [
    '', 'א׳', 'ב׳', 'ג׳', 'ד׳', 'ה׳', 'ו׳', 'ז׳', 'ח׳', 'ט׳', 'י׳',
    'י״א', 'י״ב', 'י״ג', 'י״ד', 'ט״ו', 'ט״ז', 'י״ז', 'י״ח', 'י״ט', 'כ׳',
    'כ״א', 'כ״ב', 'כ״ג', 'כ״ד', 'כ״ה', 'כ״ו', 'כ״ז', 'כ״ח', 'כ״ט', 'ל׳'
]

def clean_halacha_text(raw_html):
    comm_idx = -1
    for marker in [
        '<p></p><span style=\'font-size:80%',
        '<p></p><span style=\"font-size:80%',
        '<span style=\"background-color:#',
        '<span style=\'background-color:#',
        '<span style=\"font-size:80%',
        '<span style=\'font-size:80%',
        '<hr', '<HR'
    ]:
        c = raw_html.find(marker)
        if c != -1 and (comm_idx == -1 or c < comm_idx):
            comm_idx = c
    clean = raw_html[:comm_idx] if comm_idx != -1 else raw_html
    clean = re.sub(r'<span style=[\"\']font-size:31px;?[\"\']>.*?</span>', '', clean, flags=re.DOTALL | re.IGNORECASE)
    clean = re.sub(r'<!--.*?-->', '', clean, flags=re.DOTALL)
    clean = re.sub(r'<small><small>\[.*?\]</small></small>', '', clean, flags=re.DOTALL)
    clean = re.sub(r'<[^>]+>', '', clean)
    clean = html.unescape(clean).strip()
    clean = re.sub(r'\s+', ' ', clean)
    return clean

def strip_nikud(text):
    return re.sub(r'[\u0591-\u05BD\u05BF-\u05C2\u05C4-\u05C7]', '', text)

print("Starting extraction of all 14 books of Mishneh Torah...")
os.makedirs("app/src/main/assets/database", exist_ok=True)
db_path = "app/src/main/assets/database/rambam.db"
if os.path.exists(db_path):
    os.remove(db_path)

conn = sqlite3.connect(db_path)
cur = conn.cursor()

# Create exact Room tables
cur.execute("""CREATE TABLE IF NOT EXISTS `content_sections` (
    `sectionId` TEXT NOT NULL, 
    `workId` TEXT NOT NULL, 
    `titleHebrew` TEXT NOT NULL, 
    `chapterCount` INTEGER NOT NULL, 
    `orderIndex` INTEGER NOT NULL, 
    PRIMARY KEY(`sectionId`))""")

cur.execute("""CREATE TABLE IF NOT EXISTS `chapters` (
    `id` TEXT NOT NULL, 
    `sectionId` TEXT NOT NULL, 
    `chapterNumber` INTEGER NOT NULL, 
    `chapterHebrew` TEXT NOT NULL, 
    `halachotCount` INTEGER NOT NULL, 
    `sourceCredit` TEXT NOT NULL, 
    `license` TEXT NOT NULL, 
    PRIMARY KEY(`id`))""")

cur.execute("""CREATE TABLE IF NOT EXISTS `halachot` (
    `id` TEXT NOT NULL, 
    `chapterId` TEXT NOT NULL, 
    `sectionId` TEXT NOT NULL, 
    `chapterNumber` INTEGER NOT NULL, 
    `halachaNumber` INTEGER NOT NULL, 
    `halachaHebrew` TEXT NOT NULL, 
    `letter` TEXT NOT NULL, 
    `textWithNikud` TEXT NOT NULL, 
    `textPlain` TEXT NOT NULL, 
    `quoteFingerprint` TEXT NOT NULL, 
    PRIMARY KEY(`id`))""")

cur.execute("""CREATE TABLE IF NOT EXISTS `reading_positions` (
    `track` TEXT NOT NULL, 
    `chapterId` TEXT NOT NULL, 
    `sectionId` TEXT NOT NULL, 
    `chapterNumber` INTEGER NOT NULL, 
    `halachaId` TEXT NOT NULL, 
    `halachaIndex` INTEGER NOT NULL, 
    `textOffset` INTEGER NOT NULL, 
    `scrollOffsetFraction` REAL NOT NULL, 
    `quoteFingerprint` TEXT NOT NULL, 
    `contentVersion` TEXT NOT NULL, 
    `updatedAt` INTEGER NOT NULL, 
    PRIMARY KEY(`track`, `chapterId`))""")

cur.execute("""CREATE TABLE IF NOT EXISTS `chapter_completions` (
    `track` TEXT NOT NULL, 
    `chapterId` TEXT NOT NULL, 
    `studyDate` TEXT NOT NULL, 
    `completedAt` INTEGER NOT NULL, 
    PRIMARY KEY(`track`, `chapterId`, `studyDate`))""")

cur.execute("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
cur.execute("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '50f04c4f7d41abc7a68b0a607060d5b5')")

global_sec_order = 1
total_chaps_inserted = 0
total_halachot_inserted = 0

for b in BOOKS_CONFIG:
    url = f"https://www.toratemetfreeware.com/online/f_{b['toratEmetNum']:05d}.html"
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    print(f"Fetching {b['titleHebrew']} ({b['bookId']})...")
    with urllib.request.urlopen(req, timeout=30) as resp:
        txt = resp.read().decode('windows-1255', errors='ignore')
    
    sections = b["sections"]
    for s_idx, sec in enumerate(sections):
        sec_id = sec["sectionId"]
        sec_title = sec["titleHebrew"]
        sec_prefix = sec["prefix"]
        num_chapters = sec["numChapters"]
        
        cur.execute(
            "INSERT INTO content_sections VALUES (?, ?, ?, ?, ?)",
            (sec_id, "mishneh-torah", sec_title, num_chapters, global_sec_order)
        )
        global_sec_order += 1
        
        for c_num in range(1, num_chapters + 1):
            ord_name = heb_ordinals[c_num] if c_num < len(heb_ordinals) else str(c_num)
            chap_anchor_start = f'<a name="{sec_prefix}B-BפרקB{ord_name}">'
            start_pos = txt.find(chap_anchor_start)
            
            # If not found, try flexible search
            if start_pos == -1:
                # search regex
                pat = re.compile(rf'<a name="{re.escape(sec_prefix)}B-BפרקB[^\"]*?{re.escape(ord_name)}[^\"]*?\">')
                m = pat.search(txt)
                if m:
                    start_pos = m.start()
                else:
                    print(f"Warning: could not find chapter anchor {chap_anchor_start}")
                    start_pos = -1

            end_pos = -1
            if start_pos != -1:
                # search next anchor
                if c_num < num_chapters:
                    next_ord = heb_ordinals[c_num + 1] if c_num + 1 < len(heb_ordinals) else str(c_num + 1)
                    next_anchor = f'<a name="{sec_prefix}B-BפרקB{next_ord}">'
                    end_pos = txt.find(next_anchor, start_pos)
                else:
                    if s_idx + 1 < len(sections):
                        next_sec = sections[s_idx + 1]
                        next_anchor = f'<a name="{next_sec["prefix"]}'
                        end_pos = txt.find(next_anchor, start_pos)
                    else:
                        end_pos = len(txt)
            
            if end_pos == -1:
                end_pos = start_pos + 100000 if start_pos != -1 else -1

            chap_html = txt[start_pos:end_pos] if start_pos != -1 else ""
            
            # Find halachot
            h_pat = re.compile(rf'<a name="{re.escape(sec_prefix)}B-BפרקB[^\"]*?-([א-ת]+)"></a>')
            matches = list(h_pat.finditer(chap_html))
            
            halachot_list = []
            for h_idx, hm in enumerate(matches):
                letter = hm.group(1)
                h_start = hm.end()
                h_end = matches[h_idx + 1].start() if h_idx + 1 < len(matches) else len(chap_html)
                h_chunk = chap_html[h_start:h_end]
                
                clean_text = clean_halacha_text(h_chunk)
                if not clean_text:
                    continue
                plain_text = strip_nikud(clean_text)
                halachot_list.append((h_idx + 1, f"הלכה {letter}", letter, clean_text, plain_text))
            
            # If no halacha sub-anchors were matched (fallback for single block chapter)
            if not halachot_list and chap_html:
                clean_text = clean_halacha_text(chap_html)
                if clean_text:
                    plain_text = strip_nikud(clean_text)
                    halachot_list.append((1, "הלכה א׳", "א", clean_text, plain_text))
            
            ch_id = f"{sec_id}_{c_num}"
            ch_heb = f"פרק {hebrew_numeral_map[c_num]}" if c_num < len(hebrew_numeral_map) else f"פרק {c_num}"
            
            cur.execute(
                "INSERT INTO chapters VALUES (?, ?, ?, ?, ?, ?, ?)",
                (ch_id, sec_id, c_num, ch_heb, len(halachot_list), "תורת אמת", "CC BY-NC-SA 2.5")
            )
            total_chaps_inserted += 1
            
            for h_num, h_heb, letter, nikud, plain in halachot_list:
                h_id = f"{ch_id}_{h_num}"
                fp = plain[:30]
                cur.execute(
                    "INSERT INTO halachot VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    (h_id, ch_id, sec_id, c_num, h_num, h_heb, letter, nikud, plain, fp)
                )
                total_halachot_inserted += 1

conn.commit()
conn.close()

db_size = os.path.getsize(db_path)
print(f"DATABASE BUILD COMPLETE!")
print(f"Total chapters inserted: {total_chaps_inserted}")
print(f"Total halachot inserted: {total_halachot_inserted}")
print(f"Database file size: {db_size} bytes ({db_size / (1024*1024):.2f} MB)")
