"""Hebrew prayer text -> uroman letters for the MMS CTC aligner.

The MMS aligner was trained on uroman output of unpointed text, so words are reduced to their
consonants (measured on known recordings: clearly better separation from wrong text than a
phonetic Ashkenazi transliteration). Two measured adjustments for this reader: the divine name
(יהוה / יי, read Adoynoy) is "'dnv", and a silent final ה (no mappiq) is dropped.
Words without vowel points (labels, verse numbers, instructions) are dropped.
Used only to locate known text in audio; the displayed prayer text is never changed.
"""
import re
import unicodedata

import uroman

LETTER = re.compile('[א-ת]')
POINTS = re.compile('[ְ-ׇּׁׂ]')
_U = uroman.Uroman()


def clean(text):
    text = unicodedata.normalize('NFC', text)
    text = re.sub(r'\([^)]*\)|\[[^\]]*\]|<[^>]+>', ' ', text)
    return re.sub('[־|׀]', ' ', text)


def divine_name(skeleton):
    """Index of יהוה / יי after up to two prefix letters (לַיְיָ, וַיהֹוָה...), else None."""
    for core in ('יהוה', 'יי'):
        at = skeleton.find(core)
        if at >= 0 and at + len(core) == len(skeleton) and at <= 2 and all(c in 'ובלכמשה' for c in skeleton[:at]):
            return at
    return None


DIVINE = "'dnv"


def words(text):
    """(word as written, consonant skeleton as recited, divine-name prefix length or None)."""
    out = []
    for raw in clean(text).split():
        skeleton = ''.join(LETTER.findall(raw))
        if not skeleton:
            continue
        at = divine_name(skeleton)
        if not POINTS.search(raw) and at is None:
            continue
        if at is None and skeleton.endswith('ה') and len(skeleton) > 1 and not re.search('ה[\u05b0-\u05c7]*\u05bc', raw[-3:]):
            skeleton = skeleton[:-1]
        out.append((raw, skeleton if at is None else skeleton[:at], at))
    return out


def latin_words(text):
    result = []
    for raw, skeleton, divine in words(text):
        latin = re.sub("[^a-z']", '', _U.romanize_string(skeleton).lower()) if skeleton else ''
        if divine is not None:
            latin += DIVINE
        if latin:
            result.append((raw, latin))
    return result
