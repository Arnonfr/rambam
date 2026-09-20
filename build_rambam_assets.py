import urllib.request
import re
import html
import json
import os

url = 'https://www.toratemetfreeware.com/online/f_02017.html'
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})

print("Fetching Torat Emet Sefer Nashim HTML...")
with urllib.request.urlopen(req, timeout=30) as resp:
    raw = resp.read()

txt = raw.decode('windows-1255', errors='ignore')
print(f"Downloaded {len(txt)} chars")

sections_meta = [
    {
        "sectionId": "ishut",
        "titleHebrew": "הלכות אישות",
        "numChapters": 25,
        "prefix": "הלכותBאישות",
        "orderIndex": 1
    },
    {
        "sectionId": "gerushin",
        "titleHebrew": "הלכות גירושין",
        "numChapters": 13,
        "prefix": "הלכותBגרושין",
        "orderIndex": 2
    },
    {
        "sectionId": "yibum",
        "titleHebrew": "הלכות ייבום וחליצה",
        "numChapters": 8,
        "prefix": "הלכותBיבוםBוחליצה",
        "orderIndex": 3
    },
    {
        "sectionId": "naarah",
        "titleHebrew": "הלכות נערה בתולה",
        "numChapters": 3,
        "prefix": "הלכותBנערהBבתולה",
        "orderIndex": 4
    },
    {
        "sectionId": "sotah",
        "titleHebrew": "הלכות סוטה",
        "numChapters": 4,
        "prefix": "הלכותBסוטה",
        "orderIndex": 5
    }
]

heb_ordinals = [
    '', 'ראשון', 'שני', 'שלישי', 'רביעי', 'חמישי', 'ששי', 'שביעי', 'שמיני', 'תשיעי', 'עשירי',
    'אחדBעשר', 'שניםBעשר', 'שלשהBעשר', 'ארבעהBעשר', 'חמשהBעשר', 'ששהBעשר', 'שבעהBעשר', 'שמונהBעשר', 'תשעהBעשר', 'עשרים',
    'אחדBועשרים', 'שניםBועשרים', 'שלשהBועשרים', 'ארבעהBועשרים', 'חמשהBועשרים'
]

hebrew_numeral_map = [
    '', 'א', 'ב', 'ג', 'ד', 'ה', 'ו', 'ז', 'ח', 'ט', 'י',
    'י״א', 'י״ב', 'י״ג', 'י״ד', 'ט״ו', 'ט״ז', 'י״ז', 'י״ח', 'י״ט', 'כ',
    'כ״א', 'כ״ב', 'כ״ג', 'כ״ד', 'כ״ה'
]

# Clean text from HTML & commentary
def clean_halacha_text(raw_html):
    # Cut off commentaries
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
    # Remove letter span
    clean = re.sub(r'<span style=[\"\']font-size:31px;?[\"\']>.*?</span>', '', clean, flags=re.DOTALL | re.IGNORECASE)
    clean = re.sub(r'<!--.*?-->', '', clean, flags=re.DOTALL)
    clean = re.sub(r'<small><small>\[.*?\]</small></small>', '', clean, flags=re.DOTALL)
    clean = re.sub(r'<[^>]+>', '', clean)
    clean = html.unescape(clean).strip()
    clean = re.sub(r'\s+', ' ', clean)
    return clean

# Function to remove nikud for search/offset
def strip_nikud(text):
    return re.sub(r'[\u0591-\u05BD\u05BF-\u05C2\u05C4-\u05C7]', '', text)

all_sections = []
total_chapters_count = 0
total_halachot_count = 0

for sec_idx, sec in enumerate(sections_meta):
    section_data = {
        "sectionId": sec["sectionId"],
        "titleHebrew": sec["titleHebrew"],
        "workId": "mishneh-torah",
        "orderIndex": sec["orderIndex"],
        "chapters": []
    }
    
    for c_num in range(1, sec["numChapters"] + 1):
        ord_name = heb_ordinals[c_num]
        chap_anchor_start = f'<a name="{sec["prefix"]}B-BפרקB{ord_name}">'
        start_pos = txt.find(chap_anchor_start)
        if start_pos == -1:
            # fallback search
            print(f"Error: {chap_anchor_start} not found!")
            continue
            
        # Find next chapter or next section anchor
        end_pos = -1
        if c_num < sec["numChapters"]:
            next_ord = heb_ordinals[c_num + 1]
            next_anchor = f'<a name="{sec["prefix"]}B-BפרקB{next_ord}">'
            end_pos = txt.find(next_anchor, start_pos)
        else:
            if sec_idx + 1 < len(sections_meta):
                next_sec = sections_meta[sec_idx + 1]
                next_anchor = f'<a name="{next_sec["prefix"]}'
                end_pos = txt.find(next_anchor, start_pos)
            else:
                end_pos = len(txt)
                
        if end_pos == -1:
            end_pos = len(txt)
            
        chap_html = txt[start_pos:end_pos]
        
        # Extract halachot
        pattern = re.compile(rf'<a name="{re.escape(sec["prefix"])}B-BפרקB{re.escape(ord_name)}-([א-ת]+)"></a>')
        matches = list(pattern.finditer(chap_html))
        
        halachot_list = []
        for h_idx, m in enumerate(matches):
            letter = m.group(1)
            h_start = m.end()
            h_end = matches[h_idx + 1].start() if h_idx + 1 < len(matches) else len(chap_html)
            h_chunk = chap_html[h_start:h_end]
            
            clean_text = clean_halacha_text(h_chunk)
            if not clean_text:
                continue
                
            plain_text = strip_nikud(clean_text)
            
            halacha_obj = {
                "halachaNumber": h_idx + 1,
                "halachaHebrew": f"הלכה {letter}",
                "letter": letter,
                "textWithNikud": clean_text,
                "textPlain": plain_text,
                "quoteFingerprint": plain_text[:30]
            }
            halachot_list.append(halacha_obj)
            
        chap_heb = hebrew_numeral_map[c_num] if c_num < len(hebrew_numeral_map) else str(c_num)
        chapter_obj = {
            "chapterNumber": c_num,
            "chapterHebrew": f"פרק {chap_heb}",
            "halachotCount": len(halachot_list),
            "sourceCredit": "תורת אמת (ר׳ פנחס ראובן ור.מ.)",
            "license": "CC BY-NC-SA 2.5",
            "halachot": halachot_list
        }
        section_data["chapters"].append(chapter_obj)
        total_chapters_count += 1
        total_halachot_count += len(halachot_list)
        
    all_sections.append(section_data)

print(f"Extraction complete! Total sections: {len(all_sections)}, Total chapters: {total_chapters_count}, Total halachot: {total_halachot_count}")

out_path = "app/src/main/assets/sefer_nashim.json"
with open(out_path, "w", encoding="utf-8") as f:
    json.dump({
        "book": "sefer_nashim",
        "bookTitleHebrew": "ספר נשים",
        "attribution": {
            "source": "תורת אמת",
            "url": "https://www.toratemetfreeware.com/online/f_02017.html",
            "rightsHolders": "ר׳ פנחס ראובן ור.מ.",
            "license": "CC BY-NC-SA 2.5",
            "licenseUrl": "https://creativecommons.org/licenses/by-nc-sa/2.5/",
            "notice": "הטקסט המנוקד מופץ תחת רישיון CC BY-NC-SA 2.5 לשימוש לא מסחרי. המקור אינו תומך באפליקציה או מעניק לה חסות."
        },
        "sections": all_sections
    }, f, ensure_ascii=False, indent=2)

print(f"Saved to {out_path} ({os.path.getsize(out_path)} bytes)")
