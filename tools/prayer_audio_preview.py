"""Local listening and approval page for the v2 cuts (בדיקת-אודיו.html).

A person listens, compares with the source at the same times, and marks listening / nusach per
clip. Decisions are exported as review-decisions.json (tied to each file's SHA256) and are the
only input that can mark a clip approved (see prayer_audio_manifest.py).
"""
import datetime
import json

from prayer_audio_pipeline import ROOT
from prepare_prayer_debug_audio import STEPS

REPO = ROOT.parent / 'קוד'

ORDER = ['morning-hareni-v2', 'morning-ma-tovu-v2', 'morning-adon-olam-v2', 'korbanot-akedah-v2', 'korbanot-leolam-v2',
         'korbanot-shema-v2', 'korbanot-tamid-v2', 'korbanot-ketoret-v2', 'korbanot-eizehu-v2', 'korbanot-rabbi-yishmael-v2',
         'pzd-hodu-v2', 'pzd-after-hodu-v2', 'pzd-baruch-sheamar-v2', 'pzd-mizmor-letoda-v2', 'pzd-yehi-chevod-v2',
         'pzd-ashrei-v2', 'pzd-psalm146-v2', 'pzd-psalm147-v2', 'pzd-psalm148-v2', 'pzd-psalm149-v2', 'pzd-psalm150-v2',
         'pzd-vayvarech-v2', 'pzd-shirat-hayam-v2', 'pzd-yishtabach-v2', 'tachanun-mon-thu-v2', 'tachanun-end-v2',
         'closing-ashrei-v2', 'closing-lamnatzeach-v2', 'closing-uvaletzion-v2', 'closing-tefilah-ledavid-v2',
         'closing-beit-yaakov-v2', 'closing-psalm124-v2', 'shir-sun-v2', 'shir-mon-v2', 'shir-tue-v2', 'shir-wed-v2',
         'shir-thu-v2', 'shir-fri-v2', 'closing-hoshienu-v2', 'ending-ein-kelokeinu-v2', 'ending-ketoret-v2',
         'ending-tana-v2', 'ending-aleinu-v2', 'ending-final-verses-v2']

manifest = json.loads((ROOT / 'מיפוי/cut-manifest.json').read_text())
clips = {c['id']: c for c in manifest['clips']}
findings = manifest['findings']
data = []
for clip_id in ORDER:
    c = clips[clip_id]
    notes = list(c['boundary_flags'])
    notes += [f['note'] for f in findings if f['clip'] == clip_id]
    if c['possible_unmatched_speech']:
        notes.append('דיבור שלא הותאם לטקסט סביב: ' + ', '.join(
            f'{u["word"]} (מקור {u["source_start"]:.0f}ש׳)' for u in c['possible_unmatched_speech'][:6]))
    data.append(dict(id=c['id'], title=c['title'], file=c['file'], source_id=c['source_id'], sha256=c['sha256'],
                     start=c['source_start_seconds'], end=c['source_end_seconds'], duration=c['duration_seconds'],
                     first=c['first_words'], last=c['last_words'], breaths=len(c['breaths_attenuated']),
                     weekday=c.get('weekday'), notes=notes, status=c['status']))
packed = json.dumps(data, ensure_ascii=False).replace('<', '\\u003c')
coverage = [r for r in manifest['coverage'] if not r['clip_ids'] and r['excel_row'] <= 73]
# Scenario view: the same order file and day conditions the app uses (PrayerDayRulesTest checks the
# scenario conditions against the app's day engine).
order = json.loads((REPO / 'app/src/main/assets/prayer_audio_order.json').read_text())['steps']
scenarios = json.loads((REPO / 'app/src/test/resources/prayer_day_scenarios.json').read_text())['scenarios']
for scenario in scenarios:
    scenario['weekday'] = ['mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun'][datetime.date.fromisoformat(scenario['date']).weekday()]
step_clips = {}
for clip_id, step in STEPS.items():
    c = clips[clip_id]
    step_clips.setdefault(step, []).append(dict(id=clip_id, file=c['file'], weekday=c.get('weekday'), duration=c['duration_seconds']))
scenario_packed = json.dumps(dict(order=order, scenarios=scenarios, clips=step_clips), ensure_ascii=False).replace('<', '\\u003c')
missing = json.dumps([f'{r["excel_row"]}. {r["title"]} — {r["status"]}' for r in coverage], ensure_ascii=False).replace('<', '\\u003c')

html = '''<!doctype html><html lang="he" dir="rtl"><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>בדיקת אודיו לתפילות</title>
<style>
:root{--bg:#fffcf5;--fg:#161616;--muted:#555;--card:#ffffff;--accent:#a5e1ed;--warn:#faf583;--line:#ddd;--ok:#2f7d32;--bad:#a12a2a}
@media (prefers-color-scheme:dark){:root{--bg:#141414;--fg:#eee;--muted:#aaa;--card:#1e1e1e;--accent:#1f5964;--warn:#5a560f;--line:#333;--ok:#7fd483;--bad:#ff8a8a}}
body{font:17px system-ui;background:var(--bg);color:var(--fg);margin:0;padding:16px;line-height:1.6}
main{max-width:860px;margin:auto}h1{font-size:26px;margin:.2em 0}
button,select,textarea{font:inherit;padding:8px 14px;border:1px solid #999;border-radius:12px;background:var(--card);color:var(--fg);cursor:pointer}
textarea{width:100%;box-sizing:border-box;min-height:56px;cursor:text}
.box{padding:18px;border-radius:18px;background:var(--accent);margin:16px 0}.warn{background:var(--warn);padding:12px;border-radius:12px}
.row{display:flex;gap:10px;align-items:center;flex-wrap:wrap}audio{width:100%;margin:12px 0}
#scenarioSteps li{cursor:default;padding:6px 8px}.skip{opacity:.45}.miss{color:var(--bad)}.have{color:var(--ok)}
li{padding:10px;border-bottom:1px solid var(--line);cursor:pointer}li.active{background:var(--accent)}
small{color:var(--muted)}.ok{color:var(--ok)}.bad{color:var(--bad)}ul.notes{margin:6px 0;padding-inline-start:20px}
fieldset{border:1px solid var(--line);border-radius:12px;margin:8px 0}details{margin:16px 0}
</style><main>
<h1>בדיקת מקטעי תפילה — גרסה 2</h1>
<section class="box" id="scenarioBox"><h2>תרחיש יום — כיסוי ורצף</h2>
<p><small>אותו סדר ואותם תנאים שהאפליקציה משתמשת בהם (גיליון ״מטריצת ימים״). מקטע שנדרש ואין לו הקלטה עוצר את הנגינה.</small></p>
<div class="row"><select id="scenario"></select><label><input id="minyan" type="checkbox"> במניין</label>
<label><input id="showSkipped" type="checkbox"> הצגת מקטעים שאינם נאמרים ביום זה</label></div>
<p id="scenarioSummary"></p><audio id="scenarioPlayer" controls preload="none"></audio>
<div class="row"><button id="scenarioPlay">נגינה מההתחלה</button><span id="scenarioNow"></span></div>
<ol id="scenarioSteps"></ol></section>
<h2>אישור מקטעים</h2>
<p class="warn">החיתוכים נקבעו ביישור אקוסטי אוטומטי לטקסט תורה אור ונבדקו טכנית. הם <b>טרם נשמעו ולא אושר נוסחם</b>.
אישור נעשה רק כאן, על ידי מאזין, ונשמר בקובץ החלטות שמייצאים. מקטעים חסרים לא הושלמו ולא הוקלטו מחדש.</p>
<div class="row"><label>רשימה <select id="filter"><option value="all">כל המקטעים בסדר התפילה</option>
<option value="flag">רק מקטעים עם הערות לבדיקה</option><option value="todo">רק מקטעים שטרם הוחלט עליהם</option></select></label>
<label><input id="original" type="checkbox"> השוואה למקור באותם זמנים</label></div>
<section class="box"><h2 id="title"></h2><small id="meta"></small><audio id="player" controls preload="metadata"></audio>
<div class="row"><button id="previous">הקודם</button><button id="next">הבא</button><span id="count"></span></div>
<p><small>מתחיל ב: <span id="first"></span><br>מסתיים ב: <span id="last"></span></small></p>
<ul class="notes" id="notes"></ul>
<fieldset><legend>האזנה (גבולות, איכות, מילים חתוכות)</legend>
<label><input type="radio" name="listening" value="approved"> תקין</label>
<label><input type="radio" name="listening" value="rejected"> צריך תיקון</label></fieldset>
<fieldset><legend>נוסח (מול תהלת ה׳)</legend>
<label><input type="radio" name="nusach" value="approved"> תואם</label>
<label><input type="radio" name="nusach" value="rejected"> לא תואם / חסר / עודף</label></fieldset>
<textarea id="notesInput" placeholder="הערות (למשל: חסרה המילה… בשנייה 12)"></textarea>
</section>
<div class="row"><button id="export">ייצוא קובץ החלטות</button><small id="progress"></small></div>
<ol id="list"></ol>
<details><summary>מקטעי שחרית שאין להם הקלטה</summary><ul id="missing"></ul></details>
<p><a href="מיפוי/בדיקת-כיסוי-מקטעי-תפילה.xlsx">דוח הכיסוי באקסל</a></p>
</main><script>
const all=__DATA__, missing=__MISSING__, KEY='prayer-review-v2', S=__SCENARIOS__;
const $=id=>document.getElementById(id),player=$('player');let list=all.slice(),index=0;
let decisions={};try{decisions=JSON.parse(localStorage.getItem(KEY)||'{}')}catch(e){}
function save(){try{localStorage.setItem(KEY,JSON.stringify(decisions))}catch(e){}progress()}
function decided(c){const d=decisions[c.id];return d&&d.sha256===c.sha256&&d.listening&&d.nusach}
function progress(){$('progress').textContent=all.filter(decided).length+' מתוך '+all.length+' הוחלטו'}
function mark(c){const d=decisions[c.id];if(!d||d.sha256!==c.sha256)return '';return (d.listening==='approved'&&d.nusach==='approved')?' ✓':(d.listening==='rejected'||d.nusach==='rejected')?' ✗':' …'}
function render(){const c=list[index];$('list').replaceChildren();list.forEach((x,i)=>{const li=document.createElement('li');li.textContent=x.title+mark(x)+(x.notes.length?' ·  '+x.notes.length+' הערות':'');li.className=i===index?'active':'';li.onclick=()=>load(i,true);$('list').append(li)});
if(!c)return;$('title').textContent=c.title;$('meta').textContent=c.id+' · מקור '+c.source_id+' · '+c.start.toFixed(2)+'–'+c.end.toFixed(2)+' ש׳ · '+c.duration.toFixed(1)+' ש׳ · נשימות שהונמכו: '+c.breaths;
$('first').textContent=c.first;$('last').textContent=c.last;$('notes').replaceChildren(...c.notes.map(n=>{const li=document.createElement('li');li.textContent=n;return li}));
$('count').textContent=(index+1)+' / '+list.length;$('previous').disabled=index===0;$('next').disabled=index===list.length-1;
const d=(decisions[c.id]&&decisions[c.id].sha256===c.sha256)?decisions[c.id]:{};for(const name of ['listening','nusach'])document.querySelectorAll('input[name='+name+']').forEach(r=>r.checked=r.value===d[name]);$('notesInput').value=d.notes||'';progress()}
function update(){const c=list[index];const d=decisions[c.id]&&decisions[c.id].sha256===c.sha256?decisions[c.id]:{id:c.id,sha256:c.sha256};
for(const name of ['listening','nusach']){const r=document.querySelector('input[name='+name+']:checked');if(r)d[name]=r.value}d.notes=$('notesInput').value;d.decided_at=new Date().toISOString();decisions[c.id]=d;save();render()}
document.querySelectorAll('input[type=radio]').forEach(r=>r.onchange=update);$('notesInput').onchange=update;
function load(i,play=false,offset=0){index=i;const c=list[index];if(!c)return;player.src=$('original').checked?'מקור/audio-'+c.source_id+'.m4a':c.file;player.onloadedmetadata=()=>{player.currentTime=($('original').checked?c.start:0)+offset;if(play)player.play().catch(()=>{})};render()}
function next(){if(index+1<list.length)load(index+1,true);else player.pause()}
player.onended=next;player.ontimeupdate=()=>{if($('original').checked&&player.currentTime>=list[index].end)next()};
$('next').onclick=next;$('previous').onclick=()=>{if(index>0)load(index-1,true)};
$('original').onchange=()=>{const c=list[index],offset=Math.max(0,player.currentTime-(!$('original').checked?c.start:0));load(index,!player.paused,offset)};
$('filter').onchange=()=>{player.pause();const f=$('filter').value;list=f==='flag'?all.filter(x=>x.notes.length):f==='todo'?all.filter(x=>!decided(x)):all.slice();load(0)};
$('export').onclick=()=>{const out={schema_version:1,exported_at:new Date().toISOString(),decisions:Object.values(decisions)};const a=document.createElement('a');a.href=URL.createObjectURL(new Blob([JSON.stringify(out,null,1)],{type:'application/json'}));a.download='review-decisions.json';a.click()};
const NAMES={tachanun:'תחנון',mondayOrThursday:'שני/חמישי',roshChodesh:'ראש חודש',cholHamoed:'חול המועד',chanukah:'חנוכה',elulLedavid:'לדוד ה׳ אורי',tenDaysOfRepentance:'עשרת ימי תשובה',avinuMalkeinu:'אבינו מלכנו',mizmorLetoda:'מזמור לתודה',hallel:'הלל',hallelFull:'הלל שלם',musaf:'מוסף',fast:'תענית',purim:'פורים',torahReading:'קריאת התורה',minyan:'מניין',outOfScope:'מחוץ לתחום'};
const sp=$('scenarioPlayer');let plan=[],cursor=-1;
S.scenarios.forEach((s,i)=>{const o=document.createElement('option');o.value=i;o.textContent=s.name+' · '+s.date;$('scenario').append(o)});
function applies(step,flags){return step.when.every(c=>c[0]==='!'?!flags.has(c.slice(1)):flags.has(c))}
function clipFor(step,s){const c=S.clips[step.id]||[];return c.find(x=>!x.weekday||x.weekday===s.weekday)||null}
function buildScenario(){sp.pause();cursor=-1;$('scenarioNow').textContent='';const s=S.scenarios[$('scenario').value];const flags=new Set(s.flags);if($('minyan').checked)flags.add('minyan');
const out=flags.has('outOfScope');plan=S.order.map(st=>({...st,on:!out&&applies(st,flags),clip:clipFor(st,s)}));const req=plan.filter(p=>p.on),have=req.filter(p=>p.clip);
let first=req.findIndex(p=>!p.clip);const secs=have.reduce((a,p)=>a+p.clip.duration,0);
$('scenarioSummary').textContent=out?'יום זה מחוץ לתחום האודיו (שבת / חג / סדר נפרד) — האפליקציה אינה בונה לו רצף.':
 'תנאים: '+([...flags].map(f=>NAMES[f]||f).join(', ')||'—')+' · נדרשים '+req.length+' מקטעים · מוקלטים '+have.length+' ('+Math.round(secs/60)+' דק׳) · חסרים '+(req.length-have.length)+(first>=0?' · מהתחלה הנגינה נעצרת ב: '+req[first].title:' · רצף מלא');
$('scenarioSteps').replaceChildren();plan.forEach((p,i)=>{if(!p.on&&!$('showSkipped').checked)return;const li=document.createElement('li');li.id='step'+i;
li.className=p.on?(p.clip?'have':'miss'):'skip';li.textContent=p.title+' — '+(!p.on?'לא נאמר ביום זה'+(p.when.length?' ('+p.when.map(c=>NAMES[c.replace('!','')]||c).join(' + ')+')':''):p.clip?'מוקלט ('+p.clip.id+', '+Math.round(p.clip.duration)+' ש׳)':'חסר — הנגינה נעצרת כאן');
if(p.on&&p.clip){const b=document.createElement('button');b.textContent='▶';b.style.marginInlineStart='8px';b.onclick=()=>playFrom(i);li.append(b)}
if(p.on&&!p.clip){const nx=plan.findIndex((q,j)=>j>i&&q.on&&q.clip);if(nx>0){const b=document.createElement('button');b.textContent='המשך מ־'+plan[nx].title;b.style.marginInlineStart='8px';b.onclick=()=>playFrom(nx);li.append(b)}}
$('scenarioSteps').append(li)})}
function playFrom(i){cursor=i;const p=plan[i];document.querySelectorAll('#scenarioSteps li').forEach(l=>l.style.fontWeight='');const li=$('step'+i);if(li){li.style.fontWeight='bold';li.scrollIntoView({block:'nearest'})}
$('scenarioNow').textContent='מנגן: '+p.title;sp.src=p.clip.file;sp.play().catch(()=>{})}
sp.onended=()=>{let j=cursor+1;while(j<plan.length&&!plan[j].on)j++;if(j>=plan.length){$('scenarioNow').textContent='הרצף הסתיים';return}
if(!plan[j].clip){$('scenarioNow').textContent='נעצר: חסרה הקלטה של '+plan[j].title;cursor=j;return}playFrom(j)};
$('scenarioPlay').onclick=()=>{const i=plan.findIndex(p=>p.on);if(i<0)return;if(!plan[i].clip){$('scenarioNow').textContent='נעצר מיד: חסרה הקלטה של '+plan[i].title;return}playFrom(i)};
['scenario','minyan','showSkipped'].forEach(id=>$(id).onchange=buildScenario);buildScenario();
missing.forEach(m=>{const li=document.createElement('li');li.textContent=m;$('missing').append(li)});load(0);
</script></html>'''
(ROOT / 'בדיקת-אודיו.html').write_text(html.replace('__DATA__', packed).replace('__MISSING__', missing).replace('__SCENARIOS__', scenario_packed), encoding='utf-8')
print(ROOT / 'בדיקת-אודיו.html', len(data), 'clips')
