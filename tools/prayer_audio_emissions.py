"""CTC character emissions (MMS forced-aligner) for every source recording.

Acoustic evidence for forced alignment against known prayer text; never a transcript or an approval.
Run with the audio venv: HF_HOME=<audio>/.models .venv/bin/python tools/prayer_audio_emissions.py
"""
import numpy as np
import torch
from transformers import AutoModelForCTC
from prayer_audio_pipeline import ROOT, ffmpeg

MODEL = 'MahmoudAshraf/mms-300m-1130-forced-aligner'
RATE, CHUNK, CONTEXT = 16000, 30, 2  # seconds; context frames are discarded on both sides
OUT = ROOT / 'מיפוי' / 'יישור' / 'emissions'


def decode(file):
    raw = ffmpeg('-v', 'error', '-i', file, '-ar', str(RATE), '-ac', '1', '-f', 'f32le', 'pipe:1', binary=True)
    return np.frombuffer(raw.stdout, dtype=np.float32).copy()


def emissions(model, audio):
    frames = []
    step = CHUNK * RATE
    for start in range(0, len(audio), step):
        lo, hi = max(0, start - CONTEXT * RATE), min(len(audio), start + step + CONTEXT * RATE)
        x = torch.from_numpy(audio[lo:hi])[None]
        x = (x - x.mean()) / (x.std() + 1e-7)
        with torch.inference_mode():
            logits = model(x).logits[0]
        lp = torch.log_softmax(logits.float(), -1).numpy()
        skip = (start - lo) // 320
        keep = (min(len(audio), start + step) - start) // 320
        frames.append(lp[skip:skip + keep])
    return np.concatenate(frames)


if __name__ == '__main__':
    OUT.mkdir(parents=True, exist_ok=True)
    model = AutoModelForCTC.from_pretrained(MODEL).eval()
    for file in sorted((ROOT / 'מקור').glob('*.m4a')):
        target = OUT / (file.stem + '.npy')
        if target.exists():
            continue
        audio = decode(file)
        em = emissions(model, audio)
        np.save(target, em.astype(np.float32))
        print(file.name, f'{len(audio)/RATE:.1f}s', em.shape, flush=True)
