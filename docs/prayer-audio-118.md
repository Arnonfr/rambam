# Prayer audio v2 — aligned cuts, day rules, local build 118

Not distributed. Clips are technically checked and automatically aligned to text; **none is approved by
listening or nusach yet** (approval is recorded only through the review page, see below).

## Pipeline (tools/, run with the audio venv `תפילות-אודיו/.venv`)

1. `prayer_audio_emissions.py` — MMS CTC character emissions (`MahmoudAshraf/mms-300m-1130-forced-aligner`)
   for each original recording.
2. `prayer_audio_translit.py` — text → uroman consonants. Measured on known recordings: consonant uroman
   separates right/wrong text far better than a phonetic transliteration; the divine name is `'dnv`
   (Ashkenazi reading) and a silent final ה is dropped.
3. `prayer_audio_align.py` — `locate` (search every paragraph anywhere) and `align` (ordered forced
   alignment with gap states and paragraph-skip; skipped = not recited). Reference: Torah Or text of the app
   plus `מיפוי/יישור/reference-extra.json` (texts missing from the app, marked as comparison/hypothesis only).
4. `prayer_audio_cut.py` — cut points at the acoustic onset/decay around the aligned words, overlaps resolved
   at the quietest frame; breaths attenuated 12 dB (pause length kept); static gain to −20 LUFS with a
   peak-only lookahead limiter; AAC 96 kbps mono.
   **Audio comes from the original files.** The earlier processed FLACs summed two microphones ~0.5 ms
   apart into mono (comb filter around 0.9 kHz), applied noise reduction and fell back to dynamic loudnorm —
   reported by the listener as harder to understand. Two-microphone stereo sources now use the clear
   (close) microphone only.
5. `prayer_audio_manifest.py` — `cut-manifest.json` v2, `clips.json`, coverage workbook (sheets: sources,
   clips, coverage, missing, beyond-sheet sections, nusach findings, alternatives, superseded v1). v1 files
   copied to `מיפוי/v1`.
6. `prayer_audio_preview.py` — `בדיקת-אודיו.html`: scenario view (below) + per-clip listening/nusach
   decisions exported as `review-decisions.json`; only that file (matching SHA256) can set `approved`.
7. `prepare_prayer_debug_audio.py` — debug catalog from the manifest; release catalog stays empty.

## Day rules and order (app)

- `data/prayers/PrayerDay.kt`: Hebrew date via `android.icu.util.HebrewCalendar`; Chabad weekday conditions:
  Tachanun (list from the planning workbook incl. Chabad days; manual override parameter), Mon/Thu,
  Rosh Chodesh, Chol Hamoed (Israel default), Chanukah, Purim, observed fasts, Elul–Hoshana Rabba LeDavid,
  Ten Days, Avinu Malkeinu, Mizmor Letoda, Hallel, Musaf, Torah reading. Shabbat, Yom Tov and Tisha B'Av
  are out of scope and produce no weekday sequence.
- `assets/prayer_audio_order.json`: Shacharit step order with conditions, read by the app and the review page.
  A step whose conditions hold and has no recording stays and stops playback; the dialog offers
  "המשך מ־<next recorded>" as an explicit tap. Minyan-only steps (Kaddish, Barchu, repetition, Torah reading,
  Yehalelu) exist only with `minyan` (no UI yet; default = alone).
- `test/resources/prayer_day_scenarios.json`: the 18 day types of sheet ״מטריצת ימים״ + Shabbat, each on a real
  date with its complete condition set. `PrayerDayRulesTest` checks the engine returns exactly that set.

## Verification

- Unit (Robolectric): 22 prayer tests — scenarios, anchors resolve to displayed text, order/catalog
  consistency, no-Tachanun day plays the closing to the end, Monday stops at missing Shema and offers
  Vehu Rachum, Rosh Chodesh stops at Musaf/Barchi Nafshi, minyan steps only in minyan mode.
- Emulator (Android 15): 2 tests — reader silent until a tap; day label; real playback, pause/resume,
  stop + resume at saved position, background continuity, clip transition, stop at a missing step,
  next-available offer, dismissed stop message not re-raised.
- Release APK contains no prayer audio; debug APK 1.0.118-debug, same package and certificate as 117.

## Known gaps

Missing recordings: Modeh Ani, Birchot Hashachar/HaTorah, Shema and blessings, Amidah, Vidui, Nefilat Apayim,
Hallel, Avinu Malkeinu, Musaf, Barchi Nafshi, LeDavid (only its last verse exists), Ps. 130, all Kaddish/
minyan parts, all Mincha/Maariv. Nusach comparison is against Torah Or, not a Tehillat Hashem edition.
Order of Barchi Nafshi/LeDavid after Hoshienu follows the planning sheet and needs siddur confirmation.
