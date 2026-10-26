# Reader date, Libertinus niqqud, and inline notes

The bottom study navigation shows the weekday and Hebrew day/month, without the year. It uses the lesson's ISO date for the weekday, preserves compound months (Adar I/II), and leaves prayer section navigation unchanged.

Inline sources and editorial notes in balanced parentheses or square brackets render at 65% of the selected reading size. Nested notes and attached asterisks share the smaller size. Text content, verse numbers, quotes outside notes, and existing bold spans are preserved. The shared StudyTextBlock applies this to all study readers and to inline notes in Rashi.

Libertinus Serif regular and bold now use the official v7.051 OTF files instead of the TTF variants. The bundled TTF variants lacked GSUB, including the Hebrew composition substitutions required for shin/sin dots and combinations with dagesh. The official OTF resources contain GSUB/GPOS and Hebrew ccmp. Keep these tables when replacing font resources.

- Source release: https://github.com/alerque/libertinus/releases/tag/v7.051
- Upstream TTF defect: https://github.com/alerque/libertinus/issues/589
- License: app/src/main/assets/licenses/libertinus.txt (OFL 1.1)

Words containing cantillation or rare biblical marks continue using the existing complete-word fallback, since Libertinus does not cover all of these marks.

Regression checks: HebrewDateHelperTest, ReaderCorrectionsTest, ReaderFontSelectionTest, HebrewTextNormalizerTest, and GlobalReaderPreferencesTest. Native Android rendering screenshots of the user's Rambam and Chumash passages are written to app/build/review/reader-rambam-corrected.png and reader-chumash-corrected.png.
