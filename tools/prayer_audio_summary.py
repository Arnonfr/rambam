"""Write an accurate, reproducible handoff and fail-closed playlist draft."""
import json
from pathlib import Path
from prayer_audio_pipeline import ROOT, save
from prayer_audio_package import sections

manifest = json.loads((ROOT / 'מיפוי/cut-manifest.json').read_text())
clips = json.loads((ROOT / 'מיפוי/clips.json').read_text())['clips']
inventory = json.loads((ROOT / 'מיפוי/source-inventory.json').read_text())
processed = json.loads((ROOT / 'מיפוי/processed-files.json').read_text())
sec = sections()
covered = {r for x in clips for r in x.get('excel_rows', [])}
pending = {r for x in manifest['sources'].values() for r in x.get('excel_rows_probable', [])} - covered
extra = [x for x in clips if not x.get('excel_rows')]
assert len(sec) == 129 and len({s['id'] for s in sec}) == len(sec)
assert all((ROOT / x['file']).exists() and x['decoded_ok'] for x in clips)
assert all((ROOT / x['file']).exists() and x['decoded_ok'] for x in processed)

# A template records every planned section. Missing/unapproved candidates must stop
# production playback, never silently disappear from the daily service.
templates = {}
for s in sec:
    templates.setdefault(s['service'], []).append(dict(section_id=s['id'], title=s['title'],
        day_rule_description=s['when'], exceptions=s['exceptions'], role=s['role'],
        candidate_clip_ids=[x['id'] for x in clips if s['excel_row'] in x.get('excel_rows', [])],
        approved_clip_id=None, on_unavailable='stop_and_explain'))
save(ROOT / 'מיפוי/playlist-templates.json', dict(schema_version=1,
    status='planning_only_not_ready_for_production', enable_in_app=False,
    require_nusach_and_listening_approval=True, calendar_rules_implemented=False, services=templates))

lines = [
    '# תפילות — הקלטות, עיבוד ומפת כיסוי', '',
    'חבילת עבודה מקומית. המקורות ב־Drive ובמחשב נשארו ללא שינוי. לא הופעלה תפילה באפליקציה ולא בוצעה הפצה.', '',
    f'- מקור: {len(inventory)} הקלטות, {sum(x["duration_seconds"] for x in inventory)/60:.2f} דקות.',
    f'- כל {len(processed)} הקלטות המקור קיבלו עותקי עיבוד FLAC וקבצי AAC קלים לשמיעה; AAC מלאים יחד: {sum(x["bytes"] for x in processed)/1024**2:.2f} MiB.',
    f'- {len(clips)} קבצי מקטעים ייצאו ונבדקו בפענוח ובאורך. אלה טיוטות, לא חיתוכים שאושרו בהאזנה.',
    f'- {len(covered)} מתוך {len(sec)} שורות האקסל מקושרות לפחות לקובץ מועמד; חלקן חלקיות/משולבות ושיר של יום כולל שישה קבצים.',
    f'- {len(pending)} שורות נוספות זוהו ככל הנראה בתוך הקלטות ארוכות אך עדיין לא קיבלו חיתוך עצמאי.',
    f'- {len(sec)-len(covered)-len(pending)} שורות אינן מזוהות במיפוי הנוכחי. אין להסיק מתמלול אוטומטי לבדו שהקלטה מלאה או חסרה.',
    f'- {len(extra)} מקטעים נוספים שנמצאו בהקלטות אינם שורות עצמאיות באקסל המקורי; רשומים בגיליון נפרד.', '',
    '## איפה כל דבר', '',
    '- `מקור`: ההקלטות המקוריות עם מזהים וקובץ SHA256 במיפוי.',
    '- `עיבוד`: עותקי FLAC ללא דחיסה מאבדת נוספת. high-pass עדין, הפחתת רעש שמרנית ויישור עוצמה סביב ‎−18 LUFS, תקרת peak סביב ‎−2dBTP.',
    '- `מעובדים-לשמיעה`: כל 11 הקבצים הארוכים ב־AAC מונו 80kbps, עם שמות תוכן במטא־דאטה.',
    '- `מקטעים`: קבצים נפרדים לפי manifest, כולל נקודות חיתוך, מזהי מקור ובדיקת פענוח.',
    '- `בדיקת-אודיו.html`: נגן מקומי עם הבא/הקודם, מעבר אוטומטי והשוואה למקור באותו מיקום. כרגע רשימת בדיקה ולא תפילה מלאה.',
    '- `מיפוי/בדיקת-כיסוי-מקטעי-תפילה.xlsx`: מקורות, מקטעים, כיסוי כל 129 שורות, חלופות, איכות ומקטעים מעבר לאקסל. קישורי קבצים בגיליון מקטעי אודיו.',
    '- `מיפוי/playlist-templates.json`: תוכנית לקישור המקטעים לנגן, כולל חסרים ועצירה במקום דילוג שקט.', '',
    '## תוכן המקורות', '',
    '| מזהה | תוכן שזוהה | דקות |', '|---|---|---:|',
]
for x in inventory:
    lines.append(f'| {x["id"]} | {manifest["sources"][x["id"]]["title"]} | {x["duration_seconds"]/60:.2f} |')
lines += ['', '## מה עוד נדרש לפני הפעלה', '',
    '1. לאשר כל חיתוך בהאזנה מול נוסח תהלת השם. התמלול מתקשה במבטא האשכנזי וכולל הזיות/חזרות. אין להשתמש בו כטקסט תפילה.',
    '2. להשלים פירוק הקבצים הארוכים, ובפרט הקרבנות ופסוקי דזמרה, ולפצל קבצים שמשלבים כמה מקטעים.',
    '3. להשלים או לאתר הקלטות לברכות השחר, שמע וברכותיה, עמידה, מנחה/ערבית ותוספות יום שלא זוהו. הקבצים שסופקו אינם בסיס מוכח לסידור מוקלט מלא.',
    '4. ליישם ולהוכיח את תנאי היום: חול/חול המועד, תחנון, שני וחמישי, שיר של יום, ראש חודש, עונות, אלול ותעניות. מניין/יחיד מחייבים סדרים שונים.',
    '5. טעינה לפי דרישה ומטמון מחוץ ל־APK; נגן רקע עם Media3 ומיקום שמור. לא להפעיל רשימת מועמדים לא מאושרת כפלייליסט תפילה.', '',
    'תוכנית ההטמעה המפורטת: `קוד/docs/prayer-audio-integration.md`.']
(ROOT / 'קרא-אותי.md').write_text('\n'.join(lines) + '\n', encoding='utf-8')
print(f'QA passed: {len(clips)} clips, {len(covered)} candidate-covered rows, {len(pending)} awaiting cuts, {len(sec)-len(covered)-len(pending)} unidentified rows')
