"""Package reviewed source rows and complete daily assignments; never OCR drafts."""
import argparse
import hashlib
import json
import re
from pathlib import Path

code = Path(__file__).resolve().parents[1]
research = code.parent / 'מחקר' / 'tanya-cycle-start'
source = research / 'cycle-start-reviewed.txt'
target = code / 'app/src/main/java/com/example/domain/tanya/TanyaCycleStartData.kt'
cache = code / 'app/src/main/assets/tanya_cache.json'
parser = argparse.ArgumentParser()
parser.add_argument('--check', action='store_true')
args = parser.parse_args()
text = source.read_text()
pages = [p.strip() for p in re.split(r'(?=^@\d+$)', text, flags=re.M) if p.strip()]
numbers = [int(p.splitlines()[0][1:]) for p in pages]
assert numbers == [1] + list(range(4,57)), numbers
assert text.count('~columns') == text.count('~endcolumns') == 7
assert not any(s in text for s in ['data-overlay', 'confidence', '\t'])
ids = {int(c)*1000+int(p) for c,p in re.findall(r'\[(\d+):(\d+)\]',text)}
lessons = json.loads((research/'lessons-reviewed.json').read_text())
assert len(lessons)==60
assert lessons[0]['date']=='2026-11-29' and lessons[-1]['date']=='2027-01-27'
assert lessons[-1]['ref']=='Tanya, Part I; Likkutei Amarim 23:4'
for lesson in lessons:
 ref=lesson['ref']
 match=re.fullmatch(r'Tanya, Part I; Likkutei Amarim (\d+):(\d+)(?:-(\d+))?',ref)
 fw=re.fullmatch(r"Tanya, Part I; Likkutei Amarim, Compiler's Foreword (\d+)(?:-(\d+))?",ref)
 if match:
  ch,start,end=match.groups();end=int(end or start);start=int(start);ch=int(ch)
 elif fw:
  start,end=fw.groups();end=int(end or start);start=int(start);ch=105
 else:continue
 assert len(lesson['paragraphs'])==end-start+1,ref
 assert all(ch*1000+n in ids for n in range(start,end+1)),ref
 for p in lesson['paragraphs']:
  assert p['textWithNikud'].strip() and '[מ:' not in p['textWithNikud'] and '[פ:' not in p['textWithNikud']
output='// Source rows: מחקר/tanya-cycle-start/cycle-start-reviewed.txt\npackage com.example.domain.tanya\n\ninternal object TanyaCycleStartData {\n    val pages = listOf(\n'+',\n'.join('        """'+p+'"""' for p in pages)+'\n    )\n}\n'
existing=json.loads(cache.read_text())
for lesson in lessons:existing[lesson['date']]=lesson
cache_output=json.dumps(existing,ensure_ascii=False,indent=2)+'\n'
if args.check:
 assert target.read_text()==output,'Generated print rows are stale'
 assert json.loads(cache.read_text())==existing,'Bundled lessons are stale'
else:
 target.write_text(output)
 cache.write_text(cache_output)
print(json.dumps(dict(pages=len(pages),dailyAssignments=len(lessons),sourceSha256=hashlib.sha256(text.encode()).hexdigest(),lastDate=lessons[-1]['date']),indent=2))
