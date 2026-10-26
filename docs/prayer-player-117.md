# Optional prayer audio — local preview 117

Prayer reading is the default. Entering the reader or scrolling never creates a media controller,
requests audio focus or starts playback. The quiet “האזנה” / “אודיו · מקטעים” row is optional.
Only a deliberate play action expands the aqua control strip.

Implemented: play/pause, per-section seek, playback speed, explicit section selection,
automatic consecutive playback, media-session notification/lock-screen controls, audio focus,
headphone-disconnect pause, background playback, and separate audio bookmarks keyed by service/date.
Closing the audio returns to reading. Leaving the reader or removing the app task stops audio.
Text follows section transitions, not individual words; no karaoke synchronization is claimed.

The section queue retains missing recordings. A run contains only the consecutive available
prefix starting at the requested section. It stops and explains the next missing section;
it cannot silently skip a prayer. Explicitly choosing a later recording is allowed.

## Preview limitations (not a complete recorded siddur)

- Ten existing draft clips (six weekday songs and four closing-prayer clips) are copied into
  **debug assets only**, about 6.31 MiB. They are private, ignored by Git, and not placed in release assets.
- All are unapproved drafts; wording/cut boundaries still need listening and nusach review.
- Weekday song selection is implemented. Automatic holiday/tachanun/minyan/seasonal ordering
  is NOT implemented and is not claimed by the preview. The current imported text remains unchanged.
- The imported text is Torah Or, not a fully verified Tehillat Hashem edition. The existing
  source notice remains visible. A complete Shacharit playback requires the missing recordings
  and the calendar/text work, not merely enabling a button.
- Debug prayer navigation is open for local testing; release navigation remains “בבנייה”.
- Production catalog is empty/fail-closed. Approved HTTPS recordings can use the same schema,
  but no hosting/upload was performed in this task; offline downloading/cache is still pending.

To test locally: `תפילות → שחרית → אודיו · מקטעים → אשרי → הפעלה`.
This plays the contiguous available draft closing clips and stops at the missing Kaddish recording.
The reader's initial “מודה אני” has no recording; pressing play there correctly explains the gap.

Architecture follows Android's official [MediaSessionService background playback guide](https://developer.android.com/media/media3/session/background-playback)
and [MediaController connection guide](https://developer.android.com/media/media3/session/connect-to-media-app),
using Media3 1.10.1 from the [official release notes](https://developer.android.com/jetpack/androidx/releases/media3).

## Reproducible assets and verification

- `tools/prepare_prayer_debug_audio.py`: copies only SHA256-matched local draft exports into debug assets.
- `PrayerAudioPlanTest`: gap handling, explicit start, approval boundary, HTTPS policy, weekday selection,
  and anchors resolving to unmodified reader text.
- `PrayerAudioControlsTest` and `PrayerReaderAudioTest`: no autoplay, optional controls, and reader/picker renders.
- `PrayerAudioPlaybackTest`: actual Android emulator playback, pause/resume/seek, background continuity,
  automatic section transition, stop at a missing section, and actual reader UI playback.

This is a local development implementation. No Firebase or Google Play release was made.
