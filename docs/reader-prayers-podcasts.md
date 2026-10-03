# Reader, prayer navigation, and podcasts

## Implemented

- All five study readers reserve 220dp after the final content. The pull-to-complete indicator stays above the bottom bar, within this reserved area rather than overlapping the last paragraph.
- Hebrew presentation-form letters are selectively decomposed and normalized. Unpointed text preserves punctuation and maqaf. Tehillim without cantillation removes paseq, meteg, rafe, extraordinary dots, and inverted nun; ordinary vowels stay intact. Qamats qatan maps to ordinary qamats for font compatibility. Shir shel yom shares the same cleaning.
- The existing `serif` preference now selects bundled Romm Vilna under “דפוס תניא”, preserving stored preference IDs.
- Home settings share the date navigation row; no dedicated settings row.
- Imported Shacharit sections are a continuous reader. Bottom arrows jump between sections; the current section opens a scrollable section picker. Navigation into another service occurs only at the service boundary.
- Prayer service bookmarks use stable section/paragraph IDs, persist across dates, and migrate previous per-section saved positions. Shir shel yom continues to update for the weekday.
- Third home tab lists five podcast teachers with live RSS episode metadata and safe links to the original episode/audio. No audio or artwork is copied into the application. English language is clearly identified for Rabbi Jacobson.

## Verified podcast sources

- Shneur Ashkenazi: https://he.chabad.org/library/article_cdo/aid/5287961 — RSS https://he.chabad.org/tools/rss/itunes/seriesrss_cdo/scope/5287961/variant/30977
- Simon Jacobson: https://www.meaningfullife.com/subscribes/ — RSS https://anchor.fm/s/3a56d08/podcast/rss
- Nadav Cohen: https://podcasts.apple.com/il/podcast/id1606921229 — RSS https://feeds.captivate.fm/rnadavcohen/
- Yehuda Leib Nachmanson: https://hamishpatim.co.il/ — RSS https://feeds.transistor.fm/19817b21-c5ce-432b-82f9-37e81dfa7eff
- Shabtay Slavatitzky: https://podcasts.apple.com/il/podcast/id1609388814 — RSS https://feeds.captivate.fm/rslavatitsky/

All five live feeds were fetched and parsed successfully during development. Chabad's feed has audio enclosures but no per-episode `<link>`; secure original audio is the fallback. Parser refuses XML entities, non-HTTPS destinations, oversized downloads (>5MB), and returns at most 50 episodes.

## Not yet complete

- Weekday Mincha and Maariv need a reviewed matching Nusach Ari corpus. Existing offline material covers Shacharit and Birkat Hamazon. Chabadtext's `תפלת ערבית` is primarily Shabbat and its `סדר מנחת ערב שבת` contains pre-Shabbat additions rather than the full weekday service. Neither is imported under a misleading weekday label.
- Native podcast playback, background playback, download/cache, and playback-position persistence are not implemented. Current listening opens the publisher's episode or original audio externally.
- Additional Chabad teachers can be added only with verified publisher feeds.

## Verification

- `testDebugUnitTest assembleDebug`: 92 tests passed, no failures or skipped tests.
- Emulator: home gear/date layout, continuous Shacharit, section picker/jumps, completion indicator in empty bottom area and exit to home, podcast teacher list.
- Emulator internet/DNS is unavailable (`ping: unknown host` for both Chabad and Captivate). Thus in-app live episode fetching could not be end-to-end verified there; the error/retry/publisher fallback renders correctly. Live feed downloads were verified on the host, and Android RSS parsing is covered by tests.
- The requested UI behavior was checked using the Kotlin/Compose and Android CLI skills. No store or Firebase release was performed for this request.
