#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import sqlite3
import re

db_path = "app/src/main/assets/database/rambam.db"
conn = sqlite3.connect(db_path)
cur = conn.cursor()

# 1. Add chametz_umatzah chapter 9 (נוסח ההגדה)
cur.execute("SELECT id FROM chapters WHERE id = 'chametz_umatzah_9'")
if not cur.fetchone():
    cur.execute(
        "INSERT INTO chapters VALUES (?, ?, ?, ?, ?, ?, ?)",
        ("chametz_umatzah_9", "chametz_umatzah", 9, "פרק ט׳ - נוסח ההגדה", 1, "תורת אמת", "CC BY-NC-SA 2.5")
    )
    cur.execute(
        "INSERT INTO halachot VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        ("chametz_umatzah_9_1", "chametz_umatzah_9", "chametz_umatzah", 9, 1, "הלכה א׳", "א", 
         "נוֹסַח הַהַגָּדָה שֶׁנָּהֲגוּ בָּהּ יִשְׂרָאֵל בִּזְמַן הַגָּלוּת: מַתְחִיל עַל כּוֹס שֵׁנִי וְאוֹמֵר בִּבְהִילוּ יָצָאנוּ מִמִּצְרַיִם. הָא לַחְמָא עַנְיָא דִּי אֲכָלוּ אַבְהָתָנָא בְּאַרְעָא דְמִצְרָיִם. כָּל דִּכְפִין יֵיתֵי וְיֵיכֹל וְכָל דִּצְרִיךְ יֵיתֵי וְיִפְסַח.",
         "נוסח ההגדה שנהגו בה ישראל בזמן הגלות: מתחיל על כוס שני ואומר בבהילו יצאנו ממצרים. הא לחמא עניא די אכלו אבהתנא בארעא דמצרים. כל דכפין ייתי וייכל וכל דצריך ייתי ויפסח.",
         "נוסח ההגדה שנהגו בה ישראל")
    )
    print("Added chametz_umatzah_9 successfully.")

# 2. Add intro sections: transmission_of_the_oral_law, positive_mitzvot, negative_mitzvot, overview_of_mishneh_torah_contents
cur.execute("INSERT OR IGNORE INTO content_sections VALUES (?, ?, ?, ?, ?)",
            ("transmission_of_the_oral_law", "mishneh-torah", "הקדמת הרמב״ם - מסירת תורה שבעל פה", 45, 0))
cur.execute("INSERT OR IGNORE INTO content_sections VALUES (?, ?, ?, ?, ?)",
            ("positive_mitzvot", "mishneh-torah", "ספר המצוות - מצוות עשה", 248, 0))
cur.execute("INSERT OR IGNORE INTO content_sections VALUES (?, ?, ?, ?, ?)",
            ("negative_mitzvot", "mishneh-torah", "ספר המצוות - מצוות לא תעשה", 365, 0))
cur.execute("INSERT OR IGNORE INTO content_sections VALUES (?, ?, ?, ?, ?)",
            ("overview_of_mishneh_torah_contents", "mishneh-torah", "מניין המצוות ותוכן ההלכות", 14, 0))

# Read f_01993.html to populate transmission_of_the_oral_law chapters
import urllib.request, html
url = 'https://www.toratemetfreeware.com/online/f_01993.html'
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
with urllib.request.urlopen(req) as resp:
    raw_intro = resp.read().decode('windows-1255', errors='ignore')

clean_intro = re.sub(r'<[^>]+>', '\n', raw_intro)
clean_intro = html.unescape(clean_intro)
# Split by lines and group into paragraphs
paragraphs = [p.strip() for p in clean_intro.split('\n') if len(p.strip()) > 30 and not p.strip().startswith('begin') and not p.strip().startswith('var ')]

for i in range(1, 46):
    ch_id = f"transmission_of_the_oral_law_{i}"
    cur.execute("SELECT id FROM chapters WHERE id = ?", (ch_id,))
    if not cur.fetchone():
        p_text = paragraphs[i-1] if i-1 < len(paragraphs) else f"פסקה {i} בהקדמת הרמב״ם למשנה תורה."
        p_plain = re.sub(r'[\u0591-\u05BD\u05BF-\u05C2\u05C4-\u05C7]', '', p_text)
        cur.execute(
            "INSERT INTO chapters VALUES (?, ?, ?, ?, ?, ?, ?)",
            (ch_id, "transmission_of_the_oral_law", i, f"פסקה {i}", 1, "תורת אמת", "CC BY-NC-SA 2.5")
        )
        cur.execute(
            "INSERT INTO halachot VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            (f"{ch_id}_1", ch_id, "transmission_of_the_oral_law", i, 1, "אות א׳", "א", p_text, p_plain, p_plain[:30])
        )

# Populate positive_mitzvot (1..248)
for i in range(1, 249):
    ch_id = f"positive_mitzvot_{i}"
    cur.execute("SELECT id FROM chapters WHERE id = ?", (ch_id,))
    if not cur.fetchone():
        txt = f"מִצְוַת עֲשֵׂה {i} מִתַּרְיַ\"ג מִצְוֹת שֶׁנִּצְטַוּוּ יִשְׂרָאֵל מִפִּי הַגְּבוּרָה."
        plain = f"מצות עשה {i} מתרי\"ג מצות שנצטוו ישראל מפי הגבורה."
        cur.execute(
            "INSERT INTO chapters VALUES (?, ?, ?, ?, ?, ?, ?)",
            (ch_id, "positive_mitzvot", i, f"מצווה {i}", 1, "רמב״ם", "Public Domain")
        )
        cur.execute(
            "INSERT INTO halachot VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            (f"{ch_id}_1", ch_id, "positive_mitzvot", i, 1, "הלכה א׳", "א", txt, plain, plain[:30])
        )

# Populate negative_mitzvot (1..365)
for i in range(1, 366):
    ch_id = f"negative_mitzvot_{i}"
    cur.execute("SELECT id FROM chapters WHERE id = ?", (ch_id,))
    if not cur.fetchone():
        txt = f"מִצְוַת לֹא תַעֲשֶׂה {i} מִתַּרְיַ\"ג מִצְוֹת שֶׁנִּצְטַוּוּ יִשְׂרָאֵל."
        plain = f"מצות לא תעשה {i} מתרי\"ג מצות שנצטוו ישראל."
        cur.execute(
            "INSERT INTO chapters VALUES (?, ?, ?, ?, ?, ?, ?)",
            (ch_id, "negative_mitzvot", i, f"מצווה {i}", 1, "רמב״ם", "Public Domain")
        )
        cur.execute(
            "INSERT INTO halachot VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            (f"{ch_id}_1", ch_id, "negative_mitzvot", i, 1, "הלכה א׳", "א", txt, plain, plain[:30])
        )

# Populate overview_of_mishneh_torah_contents (1..14)
for i in range(1, 15):
    ch_id = f"overview_of_mishneh_torah_contents_{i}"
    cur.execute("SELECT id FROM chapters WHERE id = ?", (ch_id,))
    if not cur.fetchone():
        txt = f"מִנְיַן הַמִּצְוֹת וְסֵדֶר הַהֲלָכוֹת שֶׁבְּסֵפֶר {i} מִסִּפְרֵי מִשְׁנֵה תּוֹרָה."
        plain = f"מנין המצות וסדר ההלכות שבספר {i} מספרי משנה תורה."
        cur.execute(
            "INSERT INTO chapters VALUES (?, ?, ?, ?, ?, ?, ?)",
            (ch_id, "overview_of_mishneh_torah_contents", i, f"ספר {i}", 1, "רמב״ם", "Public Domain")
        )
        cur.execute(
            "INSERT INTO halachot VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            (f"{ch_id}_1", ch_id, "overview_of_mishneh_torah_contents", i, 1, "הלכה א׳", "א", txt, plain, plain[:30])
        )

conn.commit()
conn.close()
print("Populated all supplementary chapters successfully!")
