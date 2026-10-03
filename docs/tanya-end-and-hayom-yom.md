# Tanya print-layout continuation and Hayom Yom

The clean-print transcription now continues from page 296 to page 325, the
last page of Kuntres Acharon in the reference volume (Otzar 770, book 108).
It is live text with fixed source rows, not an OCR image overlay. Complete
physical pages are retained; words outside the selected daily range are grey.
Large openings, small editorial inserts, page separators, and left-aligned
catchwords use the same renderer as the previously verified pages 270–295.

Iggeret HaKodesh 28–32 and Kuntres Acharon 1–9 are mapped to paragraph IDs.
Thirty-one complete common-year assignments, 29 October–28 November 2026,
are bundled offline. Their boundaries come from the source's `[פ: ...]`
markers. This continuation does not claim print coverage for earlier pages
1–269 or for other editions.

Hayom Yom is optional under Settings → Lessons on the home screen. It shares
the paragraph reader, typography controls, completion gesture, and persistent
reading-position marker, with a separate content identity. Android ICU maps
the selected study date to a Hebrew-date key, including Adar I and Adar II.
Existing home-screen choices are preserved; enabling the new lesson is explicit.

Sources:

- Book background: https://www.chabad.org/dailystudy/hayomyom.asp
- Hebrew/Yiddish text: https://text.chabadpedia.com/index.php?title=היום_יום
- Source rights: MediaWiki siteinfo/rightsinfo, GFDL 1.3 or later.

The APK includes 414 date entries, original transparent wiki source and page
revision IDs, attribution, and the full unmodified GFDL. `build_hayom_yom_assets.py`
removes navigation/wiki syntax and displays translation tooltips in parentheses.
It does not silently substitute a different date when text is missing.

Validation: complete-year/leap-year corpus lookup over 1,120 dates; all 30
new Tanya pages without gaps; all 31 bundled lessons with full section coverage.

Sefer HaMitzvot nikud remains pending. The existing Warsaw 1883 digital text
is unpointed. A Nakdimon 0.2.1 evaluation produced substantive vocalization
errors and was not added to the release assets. No claim of proofread nikud is
made, and the underlying text was not changed.
