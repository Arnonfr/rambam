# Pull completion: circular indicator

User supplied a WhatsApp bottom-pull reference and explicitly requested a check inside a circle whose perimeter follows the pull. Replaced the 142 Material checkbox card with a compact 48dp circular surface, permanent Material check icon and determinate rounded arc. The arc follows `EndOfLessonPullState.progress` directly, including reversal. Indicator reveal height and scale follow the same progress. Confirmation uses a small spring pulse. Neutral theme colors preserve contrast in light, dark, OLED and yellow-accent readers. No broad color glow, rectangular checkbox, caption or horizontal progress line. Accessibility description announces pull / release / completed and exposes range progress.

Completion threshold, persistence and return-home order are unchanged. Released to Firebase as version 143 on 2026-10-24; receipt: `docs/firebase-release-143.json`. Eight targeted tests passed and lint reported zero Error/Fatal issues.

Code: `ReaderComponents.kt`; validation: `ReaderFeedbackTest`, `EndOfLessonPullTest`. Preview renders in `app/build/review/reader-feedback-{light,oled}.png`, `completion-pull-{partial,confirmed}.png`.
