# Home Tanya print setting and upright square book icon

- SettingsDialog exposes **תניא — צורת הדף**, independent of Tanya card visibility.
- MainActivity connects it to the existing setTanyaBookView/DataStore preference,
  also used by the reader. The default remains false, and book nikud is unchanged.
- The general font selector remains relevant to all other readers. A short note
  explains that print-layout Tanya uses the fixed book typeface.
- An imagegen draft was used to explore the less-curved silhouette. The final
  artwork is code-native vector `app_icon_square.xml`, with exact solid fills:
  orange #FF873F, lemon #F4F66A, pink #F0A0DD, sky #9FDEEF; ink #111111,
  paper #FFFCF6. These match HomeNewViews lesson card colors.
- The Android vector is the source of truth. Adaptive launchers use it with an
  18% safe-area inset and matching background. Raster exports for legacy launchers
  and Play listing are rendered from that vector, not color-estimated from an AI image.
- SquareIconTest validates interior pixels of the native Android rendering.
  Asset export is opt-in with SQUARE_ICON_EXPORT_ROOT pointing to the project root;
  ordinary tests do not modify source assets. Export is mechanical vector rendering.
- The old generated artwork is retained for history; it is no longer used by
  the adaptive launcher. This change does not publish a Play release or update
  the live store listing by itself.
- Verified: home toggle UI test and all five exact-color pixel assertions passed.
  Combined regression run: 12 tests passed, no failures/errors. `assembleDebug`
  succeeded after regenerating all legacy launcher PNGs and the 512px Play asset.
