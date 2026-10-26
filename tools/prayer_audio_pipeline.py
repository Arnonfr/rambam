"""Local, resumable prayer-audio inventory / ASR / conservative mastering.

Never modifies sources. ASR and boundaries are drafts until reviewed against prayer text.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import time

ROOT = Path('/Users/hyh/Documents/Codex/לימוד-יומי/תפילות-אודיו')
MODEL = str(ROOT / '.models' / 'small8bit')
IDS = {
    '96a701fc': '1mccEHGyVcJMYWHCBafovgmrKBJaUD61T',
    '0ddc0de1': '14enecQZhr2i36LfvPlQWhunPh7rxSSht',
    '639ebb9c': '1ETm438CD6f5mMGyXQs2UUf_-1Nm_FOL0',
    '35e24669': '15lG_5r3LAcdaFgM0A-pzagQGdJcECENR',
    '9ea66621': '1BIZS9XX_ZPx_mtistJJ2H4rOeBUUr3QA',
    '2fa72994': '1vxpLFXtQKiu3WXE2zNyQBGq8ucpBc2kF',
    'dd64d422': '1hwGmGAM0d5HkNuo79Yu2Ho8Ekk847dVf',
    'a75d860b': '1FPGBfMHQy9vvBVM4fNErWuG-P_jNG3zV',
    'ce85201d': '1gncb_mIkNvEpSlCEESVVKN-epo5qj4Sy',
    'b260d7d9': '1D79YZ850SZCvOX5lIwWTHP9wZZCsSGc-',
    '55eecb96': '1iVXcePfVRvbjTtBCEfmfuZrr_Yiac0q6',
}

def save(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2), encoding='utf-8')

def ffmpeg(*args, binary=False):
    import imageio_ffmpeg
    cmd = ['rtk', 'proxy', imageio_ffmpeg.get_ffmpeg_exe(), '-hide_banner', '-nostdin', *map(str, args)]
    return subprocess.run(cmd, check=True, capture_output=True, text=not binary)

def inventory():
    records = []
    for file in sorted((ROOT / 'מקור').glob('*.m4a')):
        metadata = subprocess.check_output(['rtk', 'proxy', 'afinfo', str(file)], text=True)
        identifier = file.stem.split('-')[1]
        records.append(dict(id=identifier, filename=file.name, drive_id=IDS[identifier],
            drive_url=f'https://drive.google.com/file/d/{IDS[identifier]}/view',
            bytes=file.stat().st_size, sha256=hashlib.sha256(file.read_bytes()).hexdigest(),
            duration_seconds=float(re.search(r'estimated duration: ([0-9.]+)', metadata).group(1)),
            original_audio_metadata=metadata, identification_status='awaiting_content_review'))
    save(ROOT / 'מיפוי' / 'source-inventory.json', records)
    print(f'{len(records)} sources, {sum(x["duration_seconds"] for x in records)/60:.2f} minutes', flush=True)

def transcribe(files):
    import mlx_whisper
    import mlx.core as mx
    mx.set_cache_limit(64 * 1024 * 1024)
    mx.set_memory_limit(int(os.environ.get('PRAYER_MODEL_MEMORY_MB', '1536')) * 1024 * 1024)
    import numpy as np
    for file in files:
        output = ROOT / 'מיפוי' / os.environ.get('PRAYER_TRANSCRIPT_DIR', 'תמלולים') / (file.stem + '.json')
        if output.exists():
            continue
        print(f'Transcribing {file.name}', flush=True)
        audio = np.frombuffer(ffmpeg('-v', 'error', '-i', file, '-ar', '16000', '-ac', '1',
            '-f', 'f32le', 'pipe:1', binary=True).stdout, dtype=np.float32).copy()
        started = time.time()
        result = mlx_whisper.transcribe(audio, path_or_hf_repo=os.environ.get('PRAYER_ASR_MODEL', MODEL),
            language='he', task='transcribe', word_timestamps=False, condition_on_previous_text=False,
            temperature=(0.0, 0.2, 0.4), logprob_threshold=None,
            compression_ratio_threshold=2.4, no_speech_threshold=None,
            without_timestamps=False, sample_len=300, verbose=None)
        result['review_status'] = 'automatic_draft_not_verified'
        result['source_filename'] = file.name
        result['model'] = os.environ.get('PRAYER_ASR_MODEL', MODEL)
        result['elapsed_seconds'] = time.time() - started
        save(output, result)
        output.with_suffix('.txt').write_text('\n'.join(
            f'[{s["start"]:.2f}–{s["end"]:.2f}] {s["text"]}' for s in result['segments']), encoding='utf-8')
        print(f'Done {file.name}: {len(result["segments"])} spans in {time.time()-started:.1f}s', flush=True)

def master(files):
    # A non-destructive candidate, not a claim that ASR or prayer content is corrected.
    cleaning = 'highpass=f=65,afftdn=nr=6:nf=-55:tn=1'
    for file in files:
        target = ROOT / 'עיבוד' / (file.stem + '-clean.flac')
        if target.exists():
            continue
        print(f'Mastering {file.name}', flush=True)
        analysis = ffmpeg('-i', file, '-af', cleaning + ',loudnorm=I=-18:TP=-2:LRA=11:print_format=json', '-f', 'null', '-')
        measured = json.JSONDecoder().raw_decode(analysis.stderr[analysis.stderr.rfind('{'):])[0]
        normalize = 'loudnorm=I=-18:TP=-2:LRA=11:linear=true:print_format=json:' + ':'.join([
            f'measured_I={measured["input_i"]}', f'measured_TP={measured["input_tp"]}',
            f'measured_LRA={measured["input_lra"]}', f'measured_thresh={measured["input_thresh"]}',
            f'offset={measured["target_offset"]}'])
        result = ffmpeg('-n', '-i', file, '-af', cleaning + ',' + normalize,
            '-ar', '48000', '-ac', '1', '-c:a', 'flac', target)
        after = json.JSONDecoder().raw_decode(result.stderr[result.stderr.rfind('{'):])[0]
        save(ROOT / 'מיפוי' / 'איכות' / (file.stem + '.json'), dict(source=file.name,
            master=target.name, filters=cleaning, normalization=normalize, before=measured, after=after,
            listening_review='pending', timing_preserved=True))
        print(f'Done {file.name}: target loudness {after.get("output_i")} LUFS', flush=True)

def identify(files):
    """Short independent windows prevent repetitive ASR from obscuring file starts."""
    import mlx_whisper
    import mlx.core as mx
    mx.set_cache_limit(64 * 1024 * 1024)
    mx.set_memory_limit(int(os.environ.get('PRAYER_MODEL_MEMORY_MB', '1536')) * 1024 * 1024)
    import numpy as np
    for file in files:
        out = ROOT / 'מיפוי' / 'חלונות-זיהוי' / (file.stem + '.json')
        if out.exists():
            continue
        audio = np.frombuffer(ffmpeg('-v', 'error', '-i', file, '-ar', '16000', '-ac', '1',
            '-f', 'f32le', 'pipe:1', binary=True).stdout, dtype=np.float32).copy()
        duration = len(audio) / 16000
        windows = []
        # Head, second independent span and tail, all precisely labelled source time.
        for start in sorted(set([0.0, min(12.0, max(0.0, duration-12)), max(0.0, duration-12)])):
            end = min(duration, start+12)
            print(f'Identify {file.name}: {start:.1f}-{end:.1f}', flush=True)
            result = mlx_whisper.transcribe(audio[int(start*16000):int(end*16000)],
                path_or_hf_repo=os.environ.get('PRAYER_ASR_MODEL', MODEL),
                language='he', word_timestamps=False, condition_on_previous_text=False,
                temperature=0.0, without_timestamps=True, sample_len=128,
                initial_prompt='תפילות בנוסח האר״י. היום יום בשבת שבו היו הלויים אומרים בבית המקדש. תהלים. ברוך אתה ה׳.', verbose=None)
            windows.append(dict(start_seconds=start, end_seconds=end, text=result['text']))
        save(out, dict(source=file.name, review_status='automatic_draft_not_verified', windows=windows))

def compress(files):
    """Lightweight processed full files for listening; not section cuts."""
    target_dir = ROOT / 'מעובדים-לשמיעה'
    target_dir.mkdir(exist_ok=True)
    titles = json.loads((ROOT / 'מיפוי/cut-manifest.json').read_text())['sources']
    out = []
    for file in files:
        source_id = file.stem.split('-')[1]
        target = target_dir / (file.stem + '-clean.m4a')
        if not target.exists():
            ffmpeg('-n', '-i', ROOT / 'עיבוד' / (file.stem + '-clean.flac'),
                '-c:a', 'aac', '-b:a', '80k', '-ar', '48000', '-ac', '1',
                '-metadata', 'title=' + titles[source_id]['title'], '-movflags', '+faststart', target)
        ffmpeg('-v', 'error', '-i', target, '-f', 'null', '-')
        out.append(dict(source_id=source_id, title=titles[source_id]['title'],
                        file=str(target.relative_to(ROOT)), bytes=target.stat().st_size,
                        sha256=hashlib.sha256(target.read_bytes()).hexdigest(), decoded_ok=True))
    save(ROOT / 'מיפוי/processed-files.json', out)
    print(f'{len(out)} processed full files; {sum(x["bytes"] for x in out)/1024**2:.2f} MiB', flush=True)

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('action', choices=['inventory', 'transcribe', 'master', 'identify', 'compress'])
    parser.add_argument('--file')
    args = parser.parse_args()
    files = sorted((ROOT / 'מקור').glob('*.m4a'), key=lambda f: f.stat().st_size)
    if args.file:
        files = [f for f in files if f.name == args.file]
        if not files:
            raise SystemExit('Unknown source file')
    {'inventory': inventory, 'transcribe': lambda: transcribe(files), 'master': lambda: master(files),
     'identify': lambda: identify(files), 'compress': lambda: compress(files)}[args.action]()
