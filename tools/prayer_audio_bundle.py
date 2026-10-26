"""Portable, UTF-8 named listening bundle; not a production prayer release."""
from zipfile import ZipFile, ZIP_DEFLATED
from prayer_audio_pipeline import ROOT

paths = ['בדיקת-אודיו.html', 'קרא-אותי.md', 'מקטעים', 'מעובדים-לשמיעה',
         'מקור', 'מיפוי/clips.json', 'מיפוי/processed-files.json',
         'מיפוי/sections.json', 'מיפוי/playlist-templates.json',
         'מיפוי/בדיקת-כיסוי-מקטעי-תפילה.xlsx']
target = ROOT / 'תפילות-לבדיקה-טיוטה-2026-10-05.zip'
files = []
for name in paths:
    path = ROOT / name
    files.extend(sorted(p for p in path.rglob('*') if p.is_file()) if path.is_dir() else [path])
assert all(p.is_file() for p in files)
with ZipFile(target, 'w', compression=ZIP_DEFLATED, compresslevel=1) as bundle:
    for path in files:
        bundle.write(path, path.relative_to(ROOT).as_posix())
with ZipFile(target) as bundle:
    assert bundle.testzip() is None
    assert 'בדיקת-אודיו.html' in bundle.namelist()
    assert all(not any(ord(c) > 127 for c in item.filename) or item.flag_bits & 0x800
               for item in bundle.infolist())
print(f'Validated {len(files)} files, UTF-8 paths, {target.stat().st_size / 1024**2:.1f} MiB')
