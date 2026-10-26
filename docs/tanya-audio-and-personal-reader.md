# Tanya audio and personal Psalm reading — 2026-10-15

The fixed Tanya reader header now contains a headphones toggle. Audio is initially hidden; tapping reveals the recording section and scrolls it into view. Hiding the section leaves an already playing recording running. The player lives outside the lazy content and is closed when leaving the reader. The existing zero-index intro slot remains, preserving saved paragraph indices. Hayom Yom has no audio toggle.

## Rabbi Shneor Ashkenazi

Primary sources:
- Public publisher podcast, also linked by Apple podcast 1593258648: https://he.chabad.org/tools/rss/itunes/seriesrss_cdo/scope/5287961/variant/30977
- Publisher series: https://he.chabad.org/library/article_cdo/aid/5287961
- Rabbi’s official lesson pages, individually mapped from https://haravshneor.co.il/

A bundled metadata catalog has 77 distinct podcast recordings, covering Likkutei Amarim chapters 1–39 and the introduction. It retains multiple lessons per chapter in publisher order. Lectures spanning chapters 16–17 and 20–21 appear under both. Matching requires the exact book and chapter; Iggeret HaKodesh 26 is never matched to Likkutei Amarim 26. If no current match exists, users can explicitly choose another chapter.

Only small metadata is bundled. Signed MP3 enclosure addresses are fetched from publisher RSS and cached for 12 hours. No audio is rehosted. A HEAD request checks audio availability before showing native playback. Some publisher URLs currently fail with HTTP 403/404; each recording therefore also has a button to its exact official lesson page, where the publisher provides video playback. Availability failure stays within the audio section and cannot block reading.

Native playback shares the existing Media3 service, position persistence, speed and seek controls. Notification artist metadata names the selected teacher. Remote source check: lesson 41, chapter 19 part 1, returned HTTP 206 audio/mpeg with a 1KB range read. Some other entries failed; do not claim all 77 streams have been verified.

## Personal Psalms

A visible “עריכה” action replaces the three-dot management menu. Reading has no completion button. All selected entries now share one continuous LazyColumn. Unique chapter texts are preloaded locally once; headings and verses follow without replacing the reader or stopping between entries. Passing the next chapter heading marks the preceding personal entry. Only pulling past the end of the entire selection shows the shared haptic/check effect, marks the remaining entries and returns home. Paragraph position is saved by sequence/date and entry ID; legacy per-entry positions can still be restored. Pull completion is disabled until content and restoration have finished. Personal and daily Tehillim completion remain separate.

## Verification

Debug APK assembled successfully. The five focused test classes passed, 12 tests total: catalog ordering/book matching/signed URL parsing, headphones show/hide, personal reader edit/pull completion, and existing daily audio and completion gesture behavior. Native audio range-check evidence is recorded above. Phone installation still requires a connected USB device; the last device inventory was empty.

## Player UI refinement

Teacher names all include “הרב”. Playback controls have a centered play/pause button, stable seek and speed controls before playback, a thin round-ended timeline and circular thumb, and separate elapsed/total times in LTR even inside the Hebrew reader. Seeking previews during the drag and commits when released. The instructional “המשך הקריאה” sentence has been removed. Speed selection is also remembered before first playback.

Verification of player refinement/continuous reading: 20 focused tests passed across TanyaPlaybackControlsTest, TanyaAudioVisibilityTest, PersonalTehillimScreenTest, PersonalTehillimTest and EndOfLessonPullTest. Coverage includes a single final completion gesture across two chapters, last verse and next heading in the same viewport, distinct per-entry completion, LTR seeking in the Hebrew UI, stable play-button placement and one seek committed per drag. Debug APK assembly and APK signature verification succeeded.
