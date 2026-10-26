# Original Hayom Yom studies and completion layout

The Hayom Yom reader now displays the original study assignments as selectable, scalable Hebrew text above the aphorism. These are static historical assignments, not the selected year's daily Chumash/Tanya schedule. The printed weekday, Hebrew date and year remain visible.

`hayom_yom_studies.json` contains 414 date keys, with source URLs for each entry. The main source is the Kehot text published at https://he.chabad.org/dailystudy/hayomyom.asp. Original Adar I and Adar II are fetched from leap-year pages, because common-year pages return Adar I. Common Adar in this app includes both sets of aphorisms, so both original study headings are displayed. On 30 Cheshvan and 30 Kislev, the repeated aphorism retains the preceding day's heading with an explicit explanation that the original month had 29 days.

All 383 original weekdays were checked against the historical Hebrew calendar. Two unavailable online entries (4 and 14 Adar II) were transcribed from PDF pages 44 and 47 of the original printed book, available at https://drive.google.com/file/d/1sSzpQkEsYAX-JyXrfvJFgqjxyzCbp1SN/view. The online weekday for 14 Adar I was corrected to Friday after checking printed page 38. Scanned images are not bundled in the app.

The completion preview now uses a bottom sheet with RTL ordering and full-width right-aligned labels. Confirm is on the right and cancel on the left. The saved completion reader has fixed completion/back actions at the bottom, with space reserved below the scrolling text. Text boundary measurements account for the separate block label. Scaffold insets are consumed once to prevent duplicate bottom spacing in reader controls.

Validation includes historical assignments remaining identical across years, offline headings for 1120 consecutive dates, both original Adars, cancel leaving storage unchanged, confirmation saving only the remaining blocks, RTL action placement and the fixed footer on a 360dp-wide screen. A native Compose screenshot covers the actual Hayom Yom reader.
