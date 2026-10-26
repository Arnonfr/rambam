# Reader viewport and zmanim preferences — 144

Requested after tester feedback on version 143.

- Home date navigation: 18dp chevrons without circular borders, retained 48dp touch areas. Centered date, 16dp side padding and 4dp vertical padding. Header still fits the tested 106dp budget at 320/360/390dp.
- Settings: a single “זמני היום” navigation row opens a dedicated internal page. MainActivity’s existing `expandZmanim` entry from the ZmanimSheet starts directly on that page. App/system back returns to general settings, then exits. Page changes reset scroll to the top; all zmanim controls are expanded directly, without another nested accordion. Civil-date preference is on this page as well.
- Added shared `rememberReaderViewportState` for Rambam, Chumash, Tehillim (independent daily/Elul tabs), and Sefer Hamitzvot. Persists the exact pixel offset within a stable-keyed text item, including offsets well beyond the old 300px cap. Restores that same viewport rather than rounding to a halacha/verse/paragraph. Uses existing ContentReadingAnchorManager in a separate `viewport_` namespace, assignment-date scoped; legacy saved section remains the fallback when no exact anchor exists. Saves after a 200ms settling delay and synchronously on pause, stop, and disposal. No first-item/offset save is allowed until actual layout info matches the stable key.
- Removed inline previous-location banners from these readers, because inserting a new banner above the paragraph shifts its lines after restoration. No line number was added to the header. Tanya/Hayom Yom already retain their existing exact list-item/offset anchors.
- Fixed Tehillim’s active-verse lookup to account for its intro slots.

Files: ReaderViewportState.kt, ReaderScreen.kt, ChumashReaderScreen.kt, TehillimReaderScreen.kt, MitzvahReaderScreen.kt, HomeScreen.kt, SettingsScreen.kt, ZmanimSettings.kt.

Validation: ReaderViewportTest destroys/remounts the reading UI after a 1100px intra-paragraph scroll and asserts the same rendered line/offset. Also covers inserted-header stable-key restoration and independent assignment dates. SettingsScreenTest covers internal entry/back. ZmanimLayoutTest covers direct entry and enabled/method/location/civil-date controls, RTL, widths and header budget. Also run RambamAudioVisibilityTest and TehillimStudyDateTest. Total: 17 tests. No physical-device validation.

Pixel offsets preserve the exact viewport with unchanged text layout; changing font size or device width can reflow text and change which words fit on a line. Existing semantics of keeping only the latest date-scoped anchor per content type remain.

Release receipt: docs/firebase-release-144.json.
