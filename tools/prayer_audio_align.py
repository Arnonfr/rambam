"""Locate known prayer text in the source recordings with CTC forced alignment.

Two passes over MMS character emissions (prayer_audio_emissions.py):
1. `locate`: every reference paragraph is searched anywhere in every source (local alignment),
   giving candidate spans and a fit score. Used to establish which prayers a recording contains.
2. `align`: an ordered paragraph list is force-aligned end to end, with free "gap" states between
   paragraphs that absorb pauses, breaths and anything not in the reference text.
Results are acoustic evidence for cut points; they never mark a recording as approved.
"""
import json
import re
import sys
from pathlib import Path

import numpy as np
from numba import njit

from prayer_audio_translit import latin_words

REPO = Path(__file__).resolve().parents[1]
AUDIO = REPO.parent / 'תפילות-אודיו'
WORK = AUDIO / 'מיפוי' / 'יישור'
VOCAB = {c: i for i, c in enumerate("<blank>|<pad>|</s>|<unk>|a|i|e|n|o|u|t|s|r|m|k|l|d|g|h|y|b|p|w|c|v|j|z|f|'|q|x".split('|'))}
FRAME = 0.02  # seconds per emission frame

# Paragraph index ranges of prayers_torah_or.json -> coverage-sheet section id (or an extra id).
GROUPS = {
    'blessings': [((0, 0), 'prayer-578180c5c29d'), ((2, 17), 'prayer-578180c5c29d'), ((19, 21), 'prayer-861cda710251'),
                  ((22, 23), 'prayer-aa06e0451982')],
    'offerings': [((1, 19), 'prayer-0950fe27235b'), ((20, 23), 'prayer-2a304ecf2eff'), ((24, 28), 'prayer-333873fd3477'),
                  ((29, 31), 'prayer-fc2962e18187'), ((32, 40), 'prayer-2f86aa7f1791'), ((41, 48), 'prayer-c0f754af681f'),
                  ((49, 50), 'prayer-79627f4878bb'), ((52, 53), 'prayer-5a828b984300')],
    'psalms': [((0, 29), 'prayer-a968e1ae99d0'), ((30, 55), 'prayer-3901452cbe19'), ((56, 66), 'prayer-272be3818443'),
               ((67, 67), 'prayer-f7c17c5efd2c'), ((68, 68), 'prayer-ceb3838ab7f1'), ((69, 91), 'prayer-fff16804b456'),
               ((92, 93), 'prayer-108c7f3b3c5b'), ((94, 113), 'prayer-6227a839b78c'), ((114, 114), 'prayer-8f6a7d6ef484'),
               ((115, 115), 'prayer-7d041c772cc0'), ((116, 116), 'prayer-46f7cd54f4eb'), ((117, 120), 'prayer-f8cc2f6c24a0'),
               ((121, 145), 'prayer-bed9497e0202'), ((146, 146), 'prayer-e78c359c4449'), ((147, 147), 'prayer-cd47a6cb0251')],
    'shema': [((1, 30), 'shema')],
    'amidah': [((0, 50), 'amidah')],
    'tachanun': [((1, 6), 'prayer-357a0df82f74'), ((7, 8), 'prayer-6503df78621a'), ((9, 9), 'prayer-357a0df82f74'),
                 ((10, 32), 'prayer-495caa92952d'), ((33, 71), 'prayer-cc88d3a4d0e8'), ((73, 88), 'prayer-fd5a06861cd5'),
                 ((89, 93), 'extra-kaddish-after-tachanun')],
    'closing': [((1, 23), 'prayer-84c921d7e7a7'), ((25, 25), 'prayer-06c1203d79f4'), ((26, 50), 'prayer-b2df9d7adcc1'),
                ((52, 52), 'prayer-1d816416a1ff'), ((54, 55), 'prayer-04fcd9e72e82'), ((57, 73), 'prayer-afe519d06292'),
                ((74, 74), 'prayer-3aa4ce638800'), ((75, 75), 'extra-psalm-124'), ((77, 77), 'prayer-50e97cbac988'),
                ((78, 78), 'extra-hoshienu'), ((79, 79), 'prayer-0e1e06bd6ae1')],
}
SHIR = {'sun': (24, None), 'mon': (48, None), 'tue': (82, None), 'wed': (94, (95, 3)), 'thu': (81, None),
        'fri': (93, None), 'elul': (27, None), 'rosh_chodesh': (104, None)}
DAY_INTRO = {'sun': 'רִאשׁוֹן', 'mon': 'שֵׁנִי', 'tue': 'שְׁלִישִׁי', 'wed': 'רְבִיעִי', 'thu': 'חֲמִישִׁי', 'fri': 'שִׁשִּׁי'}


def expand_repeats(text):
    """`ג"פ X:` (three times) is recited three times."""
    return re.sub(r'ג"פ\s*([^:]+:)', lambda m: ' '.join([m.group(1)] * 3), text)


def units():
    book = json.loads((REPO / 'app/src/main/assets/prayers_torah_or.json').read_text())
    psalms = json.loads((REPO / 'app/src/main/assets/tehillim.json').read_text())
    out = []
    for section in book['sections']:
        for (lo, hi), group in GROUPS.get(section['id'], []):
            for i in range(lo, hi + 1):
                out.append(dict(id=f"{section['id']}:{i}", group=group, text=expand_repeats(section['paragraphs'][i])))
    for day, (chapter, extra) in SHIR.items():
        verses = [re.sub(r'<[^>]+>|\{[פס]\}|&\w+;', ' ', v) for v in psalms[str(chapter)]]
        if day in DAY_INTRO:
            out.append(dict(id=f'shir:{day}:intro', group=f'shir-{day}',
                            text=f'הַיּוֹם יוֹם {DAY_INTRO[day]} בַּשַּׁבָּת שֶׁבּוֹ הָיוּ הַלְוִיִּם אוֹמְרִים בְּבֵית הַמִּקְדָּשׁ:'))
        for k, verse in enumerate(verses):
            out.append(dict(id=f'shir:{day}:{chapter}:{k+1}', group=f'shir-{day}', text=verse))
        if extra:
            for k, verse in enumerate(psalms[str(extra[0])][:extra[1]]):
                out.append(dict(id=f'shir:{day}:{extra[0]}:{k+1}', group=f'shir-{day}', text=re.sub(r'<[^>]+>', ' ', verse)))
    extra = json.loads((WORK / 'reference-extra.json').read_text())
    out += [dict(id=u['id'], group=u['group'], text=u['text']) for u in extra['units']]
    for unit in out:
        unit['words'] = latin_words(unit['text'])
    return [u for u in out if u['words']]


def tokens(words):
    ids, word_of = [], []
    for w, (_, latin) in enumerate(words):
        for ch in latin:
            ids.append(VOCAB[ch]); word_of.append(w)
    return np.array(ids, np.int64), np.array(word_of, np.int64)


@njit(cache=True)
def local_match(em, toks):
    """Best local CTC alignment of `toks` ending at each frame. Scores are log-prob deficits
    relative to the frame-wise best symbol (<= 0). Returns (score[t], start[t])."""
    T = em.shape[0]
    L = toks.shape[0]
    S = 2 * L + 1
    best = np.empty(T)
    for t in range(T):
        m = em[t, 0]
        for v in range(1, em.shape[1]):
            if em[t, v] > m:
                m = em[t, v]
        best[t] = m
    NEG = -1e18
    prev = np.full(S, NEG)
    prev_start = np.zeros(S, np.int64)
    cur = np.full(S, NEG)
    cur_start = np.zeros(S, np.int64)
    end_score = np.full(T, NEG)
    end_start = np.zeros(T, np.int64)
    for t in range(T):
        for s in range(S):
            lab = 0 if s % 2 == 0 else toks[s // 2]
            e = em[t, lab] - best[t]
            # stay
            sc = prev[s]
            st = prev_start[s]
            if s >= 1 and prev[s - 1] > sc:
                sc = prev[s - 1]; st = prev_start[s - 1]
            if s >= 2 and s % 2 == 1 and toks[s // 2] != toks[s // 2 - 1] and prev[s - 2] > sc:
                sc = prev[s - 2]; st = prev_start[s - 2]
            if s <= 1 and 0.0 > sc:  # free start
                sc = 0.0; st = t
            cur[s] = sc + e
            cur_start[s] = st
        a = cur[S - 1]
        b = cur[S - 2]
        if a >= b:
            end_score[t] = a; end_start[t] = cur_start[S - 1]
        else:
            end_score[t] = b; end_start[t] = cur_start[S - 2]
        for s in range(S):
            prev[s] = cur[s]; prev_start[s] = cur_start[s]
    return end_score, end_start


@njit(cache=True)
def viterbi(em, labels, gap_penalty, skip_cost):
    """Linear-chain CTC Viterbi. labels: >0 token, 0 blank, -1 gap (absorbs any frame at a penalty).
    A gap may jump to any later gap, skipping whole paragraphs (skip_cost[k] for paragraph k), so
    text that was not recited is reported as skipped instead of being squeezed into the audio.
    Starts in state 0 or 1, ends in the last two states. Returns the state per frame."""
    T = em.shape[0]
    S = labels.shape[0]
    NEG = -1e18
    gaps = np.where(labels == -1)[0]
    gap_rank = np.full(S, -1, np.int64)
    for k in range(gaps.shape[0]):
        gap_rank[gaps[k]] = k
    back = np.zeros((T, S), np.uint8)
    gap_from = np.zeros((T, gaps.shape[0]), np.int32)
    prev = np.full(S, NEG)
    cur = np.full(S, NEG)
    for t in range(T):
        m = em[t, 0]
        for v in range(1, em.shape[1]):
            if em[t, v] > m:
                m = em[t, v]
        run = NEG
        run_src = -1
        for s in range(S):
            lab = labels[s]
            if lab == -1:
                e = max(em[t, 0], m - gap_penalty)
            else:
                e = em[t, lab]
            if t == 0:
                cur[s] = e if s <= 1 else NEG
                if lab == -1:
                    gap_from[t, gap_rank[s]] = gap_rank[s]
                continue
            sc = prev[s]; bp = 0
            if s >= 1 and prev[s - 1] > sc:
                sc = prev[s - 1]; bp = 1
            if s >= 2 and labels[s - 1] == 0 and lab != 0 and (lab != labels[s - 2] or lab == -1) and prev[s - 2] > sc:
                sc = prev[s - 2]; bp = 2
            if lab == -1:
                k = gap_rank[s]
                src = k
                if bp == 1 or bp == 2:
                    src = -2  # came through the chain
                if k >= 1:
                    run = run - skip_cost[k - 1]
                    if run > sc:
                        sc = run; src = run_src
                gap_from[t, k] = src
                if prev[s] > run:
                    run = prev[s]; run_src = k
            cur[s] = sc + e
            back[t, s] = bp
        for s in range(S):
            prev[s] = cur[s]
    s = S - 1 if prev[S - 1] >= prev[S - 2] else S - 2
    path = np.zeros(T, np.int64)
    for t in range(T - 1, -1, -1):
        path[t] = s
        if labels[s] == -1 and t > 0:
            src = gap_from[t, gap_rank[s]]
            if src >= 0 and src != gap_rank[s]:
                s = gaps[src]
                continue
        s -= back[t, s]
    return path


def emissions(source_id):
    return np.load(WORK / 'emissions' / f'audio-{source_id}.npy').astype(np.float64)


def locate(source_id, pool, top=3):
    em = emissions(source_id)
    results = []
    for unit in pool:
        toks, _ = tokens(unit['words'])
        score, start = local_match(em, toks)
        norm = score / len(toks)
        found = []
        order = np.argsort(-norm)
        for t in order:
            if len(found) >= top or norm[t] < -3.5:
                break
            s0 = start[t]
            if any(not (t < a or s0 > b) for a, b, _ in found):
                continue
            found.append((int(s0), int(t), float(norm[t])))
        results.append(dict(id=unit['id'], group=unit['group'], tokens=len(toks),
                            hits=[dict(start=a * FRAME, end=(b + 1) * FRAME, score=round(c, 3)) for a, b, c in found]))
    return results


def align(source_id, unit_list, gap_penalty=4.0, skip_per_token=6.0):
    em = emissions(source_id)
    labels, owner = [-1], [(-1, -1)]
    for u, unit in enumerate(unit_list):
        toks, word_of = tokens(unit['words'])
        for k, tok in enumerate(toks):
            labels += [0, int(tok)]
            owner += [(u, -1), (u, int(word_of[k]))]
        labels += [0, -1]
        owner += [(u, -1), (-1, u)]
    skip_cost = np.array([skip_per_token * len(tokens(u['words'])[0]) for u in unit_list], np.float64)
    path = viterbi(em, np.array(labels, np.int64), gap_penalty, skip_cost)
    best = em.max(1)
    words = {}
    for t, s in enumerate(path):
        u, w = owner[s]
        if u >= 0 and w >= 0:
            key = (u, w)
            span = words.setdefault(key, [t, t, 0.0, 0])
            span[1] = t
            span[2] += em[t, labels[s]] - best[t]
            span[3] += 1
    out = []
    for u, unit in enumerate(unit_list):
        ws = []
        if not any((u, w) in words for w in range(len(unit['words']))):
            out.append(dict(id=unit['id'], group=unit['group'], skipped=True))
            continue
        for w, (raw, latin) in enumerate(unit['words']):
            a, b, deficit, n = words[(u, w)]
            ws.append(dict(word=raw, latin=latin, start=round(a * FRAME, 3), end=round((b + 1) * FRAME, 3),
                           fit=round(deficit / max(n, 1), 3)))
        out.append(dict(id=unit['id'], group=unit['group'], start=ws[0]['start'], end=ws[-1]['end'],
                        fit=round(float(np.mean([w['fit'] for w in ws])), 3), words=ws))
    return out


if __name__ == '__main__':
    action = sys.argv[1]
    pool = units()
    if action == 'units':
        print(len(pool), sum(len(tokens(u['words'])[0]) for u in pool))
        for u in pool[:5]:
            print(u['id'], u['group'], [w[1] for w in u['words']][:8])
    if action == 'locate':
        for source_id in sys.argv[2:]:
            result = locate(source_id, pool)
            target = WORK / 'locate' / f'{source_id}.json'
            target.parent.mkdir(exist_ok=True)
            target.write_text(json.dumps(result, ensure_ascii=False, indent=1))
            hits = sorted(((h['start'], h['end'], h['score'], r['id']) for r in result for h in r['hits']
                           if h['score'] > -1.3 and r['tokens'] >= 12), key=lambda x: x[0])
            print(f'== {source_id}: {len(hits)} confident hits')
            for a, b, s, i in hits:
                print(f'  {a:7.2f}-{b:7.2f} {s:6.2f} {i}')
    if action == 'align':
        by_id = {u['id']: u for u in pool}

        def ids(prefix, lo, hi, skip=()):
            return [f'{prefix}:{i}' for i in range(lo, hi + 1) if i not in skip and f'{prefix}:{i}' in by_id]

        def shir(day):
            return [u['id'] for u in pool if u['group'] == f'shir-{day}']

        # Final plans: units established as present by the exploratory runs. Excluded on evidence:
        # Birchot Hashachar (absent from every source), Baruch Shem (said silently), and Kaddish /
        # Yehalelu (minyan only; the reader prays alone). See docs/prayer-audio-alignment.md.
        plans = {
            '639ebb9c': ['extra:hareni', 'extra:ma_tovu', 'extra:adon_olam', 'extra:akedah_intro'] +
                        ids('offerings', 1, 19) + ['extra:ribono_akedah'] + ids('offerings', 20, 29, skip=(25,)) +
                        ['extra:yehi_ratzon_tamid'] + ids('offerings', 30, 40) + ['extra:ana_bekoach'] +
                        ids('offerings', 41, 50),
            '96a701fc': ids('psalms', 0, 146),
            '0ddc0de1': ids('tachanun', 33, 71),
            '35e24669': ids('tachanun', 73, 88) + ids('closing', 1, 75, skip=(52, 54, 55)),
            '9ea66621': ['closing:78', 'shir:elul:27:14', 'extra:ein_kelokeinu', 'extra:ketoret_intro_end',
                         'extra:ketoret_end'] + ids('offerings', 35, 37) +
                        ['extra:tana_dvei_eliyahu', 'extra:aleinu', 'extra:aleinu_2', 'extra:al_tira', 'extra:ach_tzadikim'],
        }
        for source_id, day in [('ce85201d', 'sun'), ('2fa72994', 'mon'), ('a75d860b', 'tue'), ('dd64d422', 'wed'),
                               ('55eecb96', 'thu'), ('b260d7d9', 'fri')]:
            plans[source_id] = shir(day)
        for source_id in sys.argv[2:] or plans:
            result = align(source_id, [by_id[i] for i in plans[source_id]])
            target = WORK / 'align' / f'{source_id}.json'
            target.parent.mkdir(exist_ok=True)
            target.write_text(json.dumps(dict(source_id=source_id, method='mms-ctc-forced-alignment',
                                              status='automatic_alignment_not_listening_review', units=result),
                                         ensure_ascii=False, indent=1))
            print(f'== {source_id}')
            for r in result:
                if r.get('skipped'):
                    print(f'   {"SKIPPED":>17} {r["id"]}')
                else:
                    print(f'  {r["start"]:7.2f}-{r["end"]:7.2f} {r["fit"]:6.2f} {r["id"]}')
