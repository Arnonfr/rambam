"""ASR-assisted review of small boundary regions; never automatically approve a cut."""
import json
import os
import numpy as np
import mlx.core as mx
import mlx_whisper
from prayer_audio_pipeline import ROOT, MODEL, ffmpeg, save

mx.set_cache_limit(64 * 1024 * 1024)
mx.set_memory_limit(int(os.environ.get('PRAYER_MODEL_MEMORY_MB', '1536')) * 1024 * 1024)
regions = json.loads((ROOT / 'מיפוי/boundary-regions.json').read_text())
for region in regions:
    target = ROOT / 'מיפוי' / os.environ.get('PRAYER_BOUNDARY_DIR', 'בדיקת-גבולות') / (region['id'] + '.json')
    if target.exists():
        continue
    file = ROOT / 'מקור' / ('audio-' + region['source_id'] + '.m4a')
    start, end = region['start'], region['end']
    print(region['id'], flush=True)
    audio = np.frombuffer(ffmpeg('-v', 'error', '-ss', start, '-i', file, '-t', end-start,
        '-ar', '16000', '-ac', '1', '-f', 'f32le', 'pipe:1', binary=True).stdout, dtype=np.float32).copy()
    result = mlx_whisper.transcribe(audio, path_or_hf_repo=os.environ.get('PRAYER_ASR_MODEL', MODEL), language='he',
        temperature=0.0, condition_on_previous_text=False, word_timestamps=True,
        sample_len=160, no_speech_threshold=None, initial_prompt=region['prompt'], verbose=None)
    for seg in result['segments']:
        seg['start'] += start
        seg['end'] += start
        for word in seg.get('words', []):
            word['start'] += start
            word['end'] += start
    save(target, dict(region=region, result=result, model=os.environ.get('PRAYER_ASR_MODEL', MODEL), status='draft_not_listened'))
