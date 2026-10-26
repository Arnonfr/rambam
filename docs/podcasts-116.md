# Podcasts — version 116

- Light RTL library, one search field, large square series rails.
- No teacher initials row and no invented covers or episode titles.
- Publisher artwork comes from the curated RSS feeds. See `PodcastCatalog` for the exact URLs.
- Original square artwork is fitted, not cropped; downloaded on demand, downsampled, kept in a bounded five-cover memory cache and a seven-day disk cache. Not bundled in the APK.
- Each series opens its actual RSS episodes, with loading, empty, retry and original-source actions.
- In-app audio playback with play/pause, seek and close, audio focus and headphone-disconnect handling. Play triangles are deliberately not auto-mirrored in RTL.
- This initial player is foreground-only: pauses when the app goes into the background and stops when leaving the podcasts tab. Background/lock-screen playback, downloads and a persistent listening history are not part of this iteration.
- Prayer navigation remains under construction; study screens are unchanged.
- Episode lists are capped at 50 recent feed entries and deduplicated by recording URL. Feeds have a 5 MB safety limit. Artwork has a separate 5 MB limit.
- The screenshots are actual Compose renders with publisher artwork and titles parsed from the captured original RSS feeds, not generated UI artwork.

Verification: `PodcastRepositoryTest` covers safe parsing, original audio links, duration, duplicate handling and limits; `PodcastScreenTest` covers the single search, selection, play/back/retry and optional screenshot export.

This is a local development build, not a Google Play or Firebase publication.
