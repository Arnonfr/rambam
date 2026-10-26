"""Build the LOCAL DEBUG prayer-audio catalog from the v2 cut manifest; never main/release.

Writes app/src/debug/assets/prayer_audio_debug.json and copies only SHA256-matched clips into
app/src/debug/assets/prayer-audio (git-ignored). Copies of clips no longer in the catalog are removed
from that build folder only; the clips themselves stay in תפילות-אודיו/מקטעים.
A clip is marked approved only when the manifest says listening and nusach were approved.
"""
import hashlib
import json
from pathlib import Path
import shutil

repo = Path(__file__).resolve().parents[1]
audio = repo.parent / 'תפילות-אודיו'
STEPS = {
    'morning-hareni-v2': 'hareni', 'morning-ma-tovu-v2': 'ma_tovu', 'morning-adon-olam-v2': 'adon_olam',
    'korbanot-akedah-v2': 'akedah', 'korbanot-leolam-v2': 'leolam', 'korbanot-shema-v2': 'korbanot_shema',
    'korbanot-tamid-v2': 'tamid', 'korbanot-ketoret-v2': 'ketoret', 'korbanot-eizehu-v2': 'eizehu',
    'korbanot-rabbi-yishmael-v2': 'rabbi_yishmael', 'pzd-hodu-v2': 'hodu', 'pzd-after-hodu-v2': 'after_hodu',
    'pzd-baruch-sheamar-v2': 'baruch_sheamar', 'pzd-mizmor-letoda-v2': 'mizmor_letoda', 'pzd-yehi-chevod-v2': 'yehi_chevod',
    'pzd-ashrei-v2': 'ashrei_pzd', 'pzd-psalm146-v2': 'psalm146', 'pzd-psalm147-v2': 'psalm147', 'pzd-psalm148-v2': 'psalm148',
    'pzd-psalm149-v2': 'psalm149', 'pzd-psalm150-v2': 'psalm150', 'pzd-vayvarech-v2': 'vayvarech',
    'pzd-shirat-hayam-v2': 'shirat_hayam', 'pzd-yishtabach-v2': 'yishtabach', 'tachanun-mon-thu-v2': 'vehu_rachum',
    'tachanun-end-v2': 'tachanun_end', 'closing-ashrei-v2': 'closing_ashrei', 'closing-lamnatzeach-v2': 'closing_lamnatzeach',
    'closing-uvaletzion-v2': 'closing_uvaletzion', 'closing-tefilah-ledavid-v2': 'closing_tefilah_ledavid',
    'closing-beit-yaakov-v2': 'closing_beit_yaakov', 'closing-psalm124-v2': 'closing_psalm124',
    'closing-hoshienu-v2': 'hoshienu', 'ending-ein-kelokeinu-v2': 'ein_kelokeinu', 'ending-ketoret-v2': 'ketoret_end',
    'ending-tana-v2': 'tana', 'ending-aleinu-v2': 'aleinu', 'ending-final-verses-v2': 'final_verses',
    **{f'shir-{d}-v2': 'shir_shel_yom' for d in ('sun', 'mon', 'tue', 'wed', 'thu', 'fri')},
}
WEEKDAYS = {'sun': 'SUNDAY', 'mon': 'MONDAY', 'tue': 'TUESDAY', 'wed': 'WEDNESDAY', 'thu': 'THURSDAY', 'fri': 'FRIDAY'}

if __name__ == '__main__':
    manifest = json.loads((audio / 'מיפוי/cut-manifest.json').read_text())
    order = {s['id'] for s in json.loads((repo / 'app/src/main/assets/prayer_audio_order.json').read_text())['steps']}
    target = repo / 'app/src/debug/assets/prayer-audio'
    target.mkdir(parents=True, exist_ok=True)
    entries = []
    for clip in manifest['clips']:
        step = STEPS[clip['id']]
        assert step in order, step
        source = audio / clip['file']
        assert clip['decoded_ok'] and hashlib.sha256(source.read_bytes()).hexdigest() == clip['sha256'], clip['id']
        shutil.copy2(source, target / source.name)
        entry = dict(stepId=step, id=clip['id'], uri=f'asset:///prayer-audio/{source.name}',
                     approved=bool(clip['verification']['approved']))
        if clip.get('weekday'):
            entry['weekday'] = WEEKDAYS[clip['weekday']]
        entries.append(entry)
    keep = {Path(e['uri']).name for e in entries}
    for stale in target.glob('*.m4a'):
        if stale.name not in keep:
            stale.unlink()
    catalog = repo / 'app/src/debug/assets/prayer_audio_debug.json'
    catalog.write_text('{"schemaVersion":2,"status":"draft_local_preview_only","clips":[\n' +
                       ',\n'.join(json.dumps(e, ensure_ascii=False, separators=(',', ':')) for e in entries) + '\n]}\n')
    size = sum(p.stat().st_size for p in target.glob('*.m4a')) / 1024 ** 2
    print(f'{len(entries)} clips for debug only ({sum(e["approved"] for e in entries)} approved); {size:.2f} MiB')
