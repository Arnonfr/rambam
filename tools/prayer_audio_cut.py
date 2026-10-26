"""Export prayer sections from the aligned source recordings (v2 cuts).

Cut points come from the forced alignment (prayer_audio_align.py), refined on the signal: a clip
starts at the acoustic onset of its first word and ends after the decay of its last word, inside
the silence between sections. Audible breaths between words are attenuated (not removed: pause
lengths and source timing are preserved). Sources are never modified.

Audio is taken from the ORIGINAL recording, not the earlier processed FLAC: that copy summed two
microphones ~0.5 ms apart into mono (comb filtering in the speech band), used noise reduction and
fell back to dynamic loudness normalisation, all of which made speech harder to follow. Stereo
sources use only the clearer (close) microphone. Loudness is matched with one static gain per clip
(-20 LUFS); a lookahead peak limiter touches only rare transients (measured: under 0.05% of samples,
at most ~5 dB). No compression or noise reduction.

Everything here is automatic. A clip is "technically checked" at most; listening and nusach
approval are recorded separately and only by a person.
"""
import hashlib
import json
import subprocess
import tempfile
from pathlib import Path

import numpy as np
import soundfile as sf

from prayer_audio_pipeline import ROOT, ffmpeg

WORK = ROOT / 'מיפוי' / 'יישור'
OUT = ROOT / 'מקטעים'
RATE = 48000
HOP = 480            # 10 ms analysis frames
TARGET_LUFS, PEAK_CEILING, BITRATE = -20.0, -1.5, '96k'
BREATH_GAIN_DB = -12.0

S = lambda prefix, lo, hi, skip=(): [f'{prefix}:{i}' for i in range(lo, hi + 1) if i not in skip]
# (clip id, title, coverage-sheet section id, source, unit ids, extra fields)
CLIPS = [
    ('morning-hareni-v2', 'הריני מקבל', 'extra-hareni', '639ebb9c', ['extra:hareni'], {}),
    ('morning-ma-tovu-v2', 'מה טובו', 'prayer-f9d67496c48e', '639ebb9c', ['extra:ma_tovu'], {}),
    ('morning-adon-olam-v2', 'אדון עולם', 'extra-adon-olam', '639ebb9c', ['extra:adon_olam'], {}),
    ('korbanot-akedah-v2', 'עקדה', 'prayer-0950fe27235b', '639ebb9c', ['extra:akedah_intro'] + S('offerings', 1, 19) + ['extra:ribono_akedah'], {}),
    ('korbanot-leolam-v2', 'לעולם יהא אדם', 'prayer-2a304ecf2eff', '639ebb9c', S('offerings', 20, 23), {}),
    ('korbanot-shema-v2', 'קריאת שמע קצרה', 'prayer-333873fd3477', '639ebb9c', S('offerings', 24, 28, skip=(25,)), {}),
    ('korbanot-tamid-v2', 'פרשת התמיד', 'prayer-fc2962e18187', '639ebb9c', S('offerings', 29, 31) + ['extra:yehi_ratzon_tamid'], {}),
    ('korbanot-ketoret-v2', 'קטורת', 'prayer-2f86aa7f1791', '639ebb9c', S('offerings', 32, 40) + ['extra:ana_bekoach'], {}),
    ('korbanot-eizehu-v2', 'איזהו מקומן', 'prayer-c0f754af681f', '639ebb9c', S('offerings', 41, 48), {}),
    ('korbanot-rabbi-yishmael-v2', 'ברייתא דרבי ישמעאל', 'prayer-79627f4878bb', '639ebb9c', S('offerings', 49, 50), {}),
    ('pzd-hodu-v2', 'הודו', 'prayer-a968e1ae99d0', '96a701fc', S('psalms', 0, 29), {}),
    ('pzd-after-hodu-v2', 'רוממו ומזמור שיר', 'prayer-3901452cbe19', '96a701fc', S('psalms', 30, 55), {}),
    ('pzd-baruch-sheamar-v2', 'ברוך שאמר', 'prayer-272be3818443', '96a701fc', S('psalms', 56, 66), {}),
    ('pzd-mizmor-letoda-v2', 'מזמור לתודה', 'prayer-f7c17c5efd2c', '96a701fc', ['psalms:67'], {}),
    ('pzd-yehi-chevod-v2', 'יהי כבוד', 'prayer-ceb3838ab7f1', '96a701fc', ['psalms:68'], {}),
    ('pzd-ashrei-v2', 'אשרי', 'prayer-fff16804b456', '96a701fc', S('psalms', 69, 91), {}),
    ('pzd-psalm146-v2', 'הללויה הללי נפשי', 'prayer-108c7f3b3c5b', '96a701fc', S('psalms', 92, 93), {}),
    ('pzd-psalm147-v2', 'הללויה כי טוב', 'prayer-6227a839b78c', '96a701fc', S('psalms', 94, 113), {}),
    ('pzd-psalm148-v2', 'הללויה הללו את ה׳ מן השמים', 'prayer-8f6a7d6ef484', '96a701fc', ['psalms:114'], {}),
    ('pzd-psalm149-v2', 'הללויה שירו לה׳', 'prayer-7d041c772cc0', '96a701fc', ['psalms:115'], {}),
    ('pzd-psalm150-v2', 'הללויה הללו אל בקדשו', 'prayer-46f7cd54f4eb', '96a701fc', ['psalms:116'], {}),
    ('pzd-vayvarech-v2', 'ויברך דוד', 'prayer-f8cc2f6c24a0', '96a701fc', S('psalms', 117, 120), {}),
    ('pzd-shirat-hayam-v2', 'שירת הים', 'prayer-bed9497e0202', '96a701fc', S('psalms', 121, 145), {}),
    ('pzd-yishtabach-v2', 'ישתבח', 'prayer-e78c359c4449', '96a701fc', ['psalms:146'], {}),
    ('tachanun-mon-thu-v2', 'והוא רחום — שני וחמישי', 'prayer-cc88d3a4d0e8', '0ddc0de1', S('tachanun', 33, 71), {}),
    ('tachanun-end-v2', 'אבינו מלכנו ואנחנו לא נדע', 'prayer-fd5a06861cd5', '35e24669', S('tachanun', 73, 88), {}),
    ('closing-ashrei-v2', 'אשרי', 'prayer-84c921d7e7a7', '35e24669', S('closing', 1, 23), {}),
    ('closing-lamnatzeach-v2', 'למנצח', 'prayer-06c1203d79f4', '35e24669', ['closing:25'], {}),
    ('closing-uvaletzion-v2', 'ובא לציון', 'prayer-b2df9d7adcc1', '35e24669', S('closing', 26, 50), {}),
    ('closing-tefilah-ledavid-v2', 'תפילה לדוד', 'prayer-afe519d06292', '35e24669', S('closing', 57, 73), {}),
    ('closing-beit-yaakov-v2', 'בית יעקב', 'prayer-3aa4ce638800', '35e24669', ['closing:74'], {}),
    ('closing-psalm124-v2', 'שיר המעלות לולי ה׳', 'extra-psalm-124', '35e24669', ['closing:75'], {}),
    ('closing-hoshienu-v2', 'הושיענו', 'extra-hoshienu', '9ea66621', ['closing:78'], {}),
    ('ending-ein-kelokeinu-v2', 'אין כאלהינו', 'extra-ein-kelokeinu', '9ea66621', ['extra:ein_kelokeinu'], {}),
    ('ending-ketoret-v2', 'פיטום הקטורת', 'extra-ketoret-end', '9ea66621',
     ['extra:ketoret_intro_end', 'extra:ketoret_end', 'offerings:35'], {}),
    ('ending-tana-v2', 'תנא דבי אליהו', 'extra-tana', '9ea66621', ['extra:tana_dvei_eliyahu'], {}),
    ('ending-aleinu-v2', 'עלינו', 'prayer-373587f5d0b1', '9ea66621', ['extra:aleinu', 'extra:aleinu_2'], {}),
    ('ending-final-verses-v2', 'אל תירא ואך צדיקים', 'prayer-81077e198e5e', '9ea66621',
     ['extra:al_tira', 'extra:ach_tzadikim'], {}),
] + [
    (f'shir-{day}-v2', f'שיר של יום — {name}', 'prayer-50e97cbac988', source, [f'shir:{day}:'], {'weekday': day})
    for day, name, source in [('sun', 'ראשון', 'ce85201d'), ('mon', 'שני', '2fa72994'), ('tue', 'שלישי', 'a75d860b'),
                              ('wed', 'רביעי', 'dd64d422'), ('thu', 'חמישי', '55eecb96'), ('fri', 'שישי', 'b260d7d9')]
]


def decode(path, rate=RATE):
    raw = ffmpeg('-v', 'error', '-i', path, '-ar', str(rate), '-ac', '1', '-f', 'f32le', 'pipe:1', binary=True).stdout
    return np.frombuffer(raw, np.float32).copy()


def source_signal(source_id):
    """The original recording; for two-microphone stereo files, the channel with more speech-band detail."""
    path = ROOT / 'מקור' / f'audio-{source_id}.m4a'
    raw = ffmpeg('-v', 'error', '-i', path, '-ar', str(RATE), '-f', 'f32le', 'pipe:1', binary=True)
    probe = ffmpeg('-v', 'info', '-i', path, '-f', 'null', '-t', '0.1', '-').stderr
    x = np.frombuffer(raw.stdout, np.float32)
    if 'stereo' not in probe:
        return x.copy(), 'mono'
    x = x.reshape(-1, 2)
    def presence(v):
        spec = np.abs(np.fft.rfft(v[:RATE * 60])) ** 2
        f = np.fft.rfftfreq(min(len(v), RATE * 60), 1 / RATE)
        return spec[(f > 2000) & (f < 6000)].sum() / spec.sum()
    best = int(np.argmax([presence(x[:, 0]), presence(x[:, 1])]))
    return x[:, best].copy(), ('left' if best == 0 else 'right') + ' channel of stereo (two microphones)'


def frame_db(signal):
    n = len(signal) // HOP
    frames = signal[:n * HOP].reshape(n, HOP)
    return 10 * np.log10(np.mean(frames ** 2, axis=1) + 1e-12)


def centroid(signal):
    n = len(signal) // HOP
    frames = signal[:n * HOP].reshape(n, HOP) * np.hanning(HOP)
    spec = np.abs(np.fft.rfft(frames, axis=1))
    freqs = np.fft.rfftfreq(HOP, 1 / RATE)
    return (spec * freqs).sum(1) / (spec.sum(1) + 1e-9)


def unit_words(alignment, unit_ids):
    units = []
    for unit in alignment['units']:
        if any(unit['id'] == u or (u.endswith(':') and unit['id'].startswith(u)) for u in unit_ids):
            units.append(unit)
    return units


def run_bounds(db, centre, quiet, lo, hi, direction):
    """Walk from frame `centre` in `direction` while the signal stays above `quiet`; at most to lo/hi."""
    f = centre
    while lo < f < hi and db[f] > quiet:
        f += direction
    return f


def plan_clip(clip, alignment, db, floor, offset, neighbours):
    """Frame range and boundary checks for one clip (before overlap resolution)."""
    clip_id, title, section, source_id, unit_ids, extra = clip
    units = unit_words(alignment, unit_ids)
    skipped = [u['id'] for u in units if u.get('skipped')]
    present = [u for u in units if not u.get('skipped')]
    words = [w for u in present for w in u['words']]
    first, last = words[0]['start'] + offset, words[-1]['end'] + offset
    prev_end, next_start = neighbours
    quiet = floor + 8
    f0, f1 = int(first * 100), int(last * 100)
    lo_limit = int(max(prev_end + offset + 0.05 if prev_end is not None else 0, first - 0.6) * 100)
    hi_limit = int(min(next_start + offset - 0.08 if next_start is not None else len(db) / 100, last + 1.2) * 100)
    onset = run_bounds(db, f0, quiet, max(lo_limit, f0 - 30), f0 + 1, -1)
    start = max(lo_limit, onset - 8)            # up to 80 ms of the preceding silence
    while start > max(lo_limit, onset - 15) and db[start - 1] < quiet:
        start -= 1
    decay = run_bounds(db, f1, quiet, f1 - 1, min(hi_limit, f1 + 60), 1)
    end = min(hi_limit, decay + 12)
    while end < min(hi_limit, decay + 25) and db[end] < quiet:
        end += 1
    flags = []
    if prev_end is not None and (f0 - int((prev_end + offset) * 100)) < 12:
        flags.append('צמוד למקטע הקודם — מעט שקט לפני ההתחלה')
    if next_start is not None and (int((next_start + offset) * 100) - f1) < 12:
        flags.append('צמוד למקטע הבא — מעט שקט אחרי הסוף')
    edge_fit = [w['fit'] for w in words[:3] + words[-3:]]
    if min(edge_fit) < -3.0:
        flags.append('התאמה חלשה במילות הקצה — בדיקת גבול בהאזנה')
    for w in words[:3] + words[-3:]:
        if w['end'] - w['start'] > 1.6:
            flags.append(f'מילת קצה ארוכה מהצפוי ({w["word"]}) — ייתכן טקסט נוסף בגבול')
    return dict(clip=clip, units=units, skipped=skipped, words=words, f0=f0, f1=f1, start=start, end=end,
                flags=flags, quiet=quiet)


def resolve_overlaps(plans, db):
    """Back-to-back sections share one cut at the quietest frame between the two words."""
    plans.sort(key=lambda p: p['start'])
    for a, b in zip(plans, plans[1:]):
        if a['end'] > b['start']:
            lo, hi = min(a['f1'], b['start']), max(b['f0'], a['end'])
            lo, hi = min(lo, hi), max(lo, hi)
            cut = lo + int(np.argmin(db[lo:hi + 1]))
            a['end'] = b['start'] = cut
            note = 'אין הפסקה בין המקטעים — חיתוך משותף בנקודה השקטה ביותר; בדיקת גבול בהאזנה'
            a['flags'].append(note); b['flags'].append(note)


def render_clip(plan, signal, db, cent, floor, offset):
    clip_id, title, section, source_id, unit_ids, extra = plan['clip']
    units, skipped, words, f0, f1 = plan['units'], plan['skipped'], plan['words'], plan['f0'], plan['f1']
    start, end, flags, quiet = plan['start'], plan['end'], plan['flags'], plan['quiet']
    audio = signal[start * HOP:end * HOP].astype(np.float64).copy()

    # Breaths: quiet, noise-like (high spectral centroid) bursts that are separated from speech on both
    # sides and stay clear of every word onset/ending (where /h/, /s/ and /sh/ live).
    breaths = []
    gain = np.ones(len(audio))
    speech_db = float(np.median(db[f0:f1][db[f0:f1] > quiet])) if f1 > f0 else -20.0
    ceiling = speech_db - 12
    protected = np.zeros(len(db), bool)
    for w in words:
        protected[max(0, int((w['start'] + offset) * 100) - 15):int((w['end'] + offset) * 100) + 15] = True
    f = start + 5
    while f < end - 5:
        if floor + 6 < db[f] < ceiling and not protected[f]:
            e = f
            while e < end - 5 and floor + 6 < db[e] < ceiling and not protected[e]:
                e += 1
            if (6 <= e - f <= 70 and np.median(cent[f:e]) > 1500 and db[f - 5:f].max() < ceiling
                    and db[e:e + 5].max() < ceiling):
                breaths.append((f, e))
            f = e
        f += 1
    ramp = int(0.025 * RATE)
    attenuation = 10 ** (BREATH_GAIN_DB / 20)
    for f, e in breaths:
        s0, s1 = (f - start) * HOP, (e - start) * HOP
        env = np.ones(s1 - s0 + 2 * ramp)
        env[:] = attenuation
        env[:ramp] = np.linspace(1, attenuation, ramp)
        env[-ramp:] = np.linspace(attenuation, 1, ramp)
        a0 = max(0, s0 - ramp)
        gain[a0:a0 + len(env)] = np.minimum(gain[a0:a0 + len(env)], env[:len(gain[a0:a0 + len(env)])])
    audio *= gain
    fade_in, fade_out = int(0.012 * RATE), int(0.040 * RATE)
    audio[:fade_in] *= np.linspace(0, 1, fade_in)
    audio[-fade_out:] *= np.linspace(1, 0, fade_out)

    # Long words inside the clip: speech that the reference text did not account for.
    unmatched = [dict(word=w['word'], source_start=w['start'], seconds=round(w['end'] - w['start'], 2))
                 for w in words if w['end'] - w['start'] > max(1.6, 0.22 * len(w['latin']) + 0.8)]
    weak = [dict(word=w['word'], source_start=w['start'], fit=w['fit']) for w in words if w['fit'] < -3.5]
    return dict(
        id=clip_id, title=title, section_id=section, source_id=source_id, **extra,
        units=[u['id'] for u in units], units_not_found=skipped,
        source_start_seconds=round(start / 100 - offset, 3), source_end_seconds=round(end / 100 - offset, 3),
        first_words=' '.join(w['word'] for w in words[:4]), last_words=' '.join(w['word'] for w in words[-4:]),
        alignment_mean_fit=round(float(np.mean([w['fit'] for w in words])), 3),
        boundary_flags=flags, breaths_attenuated=[[round((f - start) / 100, 2), round((e - start) / 100, 2)] for f, e in breaths],
        possible_unmatched_speech=unmatched, weak_words=weak[:40], weak_word_count=len(weak),
    ), audio.astype(np.float32)


def loudness(path):
    result = subprocess.run(['rtk', 'proxy', __import__('imageio_ffmpeg').get_ffmpeg_exe(), '-hide_banner', '-nostdin',
                             '-i', str(path), '-af', 'ebur128=peak=true', '-f', 'null', '-'],
                            capture_output=True, text=True)
    tail = result.stderr[result.stderr.rfind('Summary:'):]
    integrated = float(tail.split('I:')[1].split('LUFS')[0])
    peak = float(tail.split('Peak:')[1].split('dBFS')[0])
    return integrated, peak


def export(meta, audio):
    with tempfile.TemporaryDirectory() as tmp:
        raw = Path(tmp) / 'raw.wav'
        sf.write(raw, audio, RATE, subtype='FLOAT')
        before, peak = loudness(raw)
        # One static gain per clip; the limiter only catches isolated peaks above the ceiling.
        gain = TARGET_LUFS - before
        gained = Path(tmp) / 'gain.wav'
        sf.write(gained, (audio * 10 ** (gain / 20)).astype(np.float32), RATE, subtype='FLOAT')
        normalized = Path(tmp) / 'norm.wav'
        ffmpeg('-y', '-i', gained, '-af', f'alimiter=limit={10 ** (PEAK_CEILING / 20):.4f}:attack=2:release=40:level=false',
               '-c:a', 'pcm_f32le', normalized)
        after, peak_after = loudness(normalized)
        target = OUT / f'{meta["id"]}.m4a'
        ffmpeg('-y', '-i', normalized, '-c:a', 'aac', '-b:a', BITRATE, '-ar', str(RATE), '-ac', '1',
               '-metadata', f'title={meta["title"]}', '-movflags', '+faststart', target)
    decoded = decode(target)
    meta.update(file=str(target.relative_to(ROOT)), duration_seconds=round(len(decoded) / RATE, 3),
                bytes=target.stat().st_size, sha256=hashlib.sha256(target.read_bytes()).hexdigest(),
                loudness_before_lufs=round(before, 2), gain_db=round(gain, 2), loudness_lufs=round(after, 2),
                true_peak_dbfs=round(peak_after, 2), decoded_ok=abs(len(decoded) / RATE - len(audio) / RATE) < 0.1)
    return meta


def main():
    results = []
    by_source = {}
    for clip in CLIPS:
        by_source.setdefault(clip[3], []).append(clip)
    for source_id, clips in by_source.items():
        alignment = json.loads((WORK / 'align' / f'{source_id}.json').read_text())
        signal, channel = source_signal(source_id)
        db, cent = frame_db(signal), centroid(signal)
        floor = float(np.percentile(db, 5))
        offset = 0.0  # alignment and audio both use the original timeline
        spans = []
        for clip in clips:
            ws = [w for u in unit_words(alignment, clip[4]) if not u.get('skipped') for w in u['words']]
            spans.append((ws[0]['start'], ws[-1]['end']))
        # Neighbours are any aligned paragraphs of the source, not only exported clips, so a cut never
        # reaches into unexported speech (e.g. unidentified text between two sections).
        edges = sorted((u['start'], u['end']) for u in alignment['units'] if not u.get('skipped'))
        plans = []
        for clip, (a, b) in zip(clips, spans):
            prev_end = max((e for s, e in edges if e <= a + 1e-6), default=None)
            next_start = min((s for s, e in edges if s >= b - 1e-6), default=None)
            plans.append(plan_clip(clip, alignment, db, floor, offset, (prev_end, next_start)))
        resolve_overlaps(plans, db)
        for plan in plans:
            meta, audio = render_clip(plan, signal, db, cent, floor, offset)
            meta['audio_source'] = f'original {channel}; static gain only'
            results.append(export(meta, audio))
            print(f'{meta["id"]:30} {meta["source_start_seconds"]:8.2f}-{meta["source_end_seconds"]:8.2f} '
                  f'{meta["duration_seconds"]:7.2f}s breaths={len(meta["breaths_attenuated"]):3} '
                  f'flags={len(meta["boundary_flags"])} unmatched={len(meta["possible_unmatched_speech"])}', flush=True)
    (WORK / 'cuts-v2.json').write_text(json.dumps(results, ensure_ascii=False, indent=1))


if __name__ == '__main__':
    main()
