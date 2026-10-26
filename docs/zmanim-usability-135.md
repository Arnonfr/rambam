# גרסה 135 — שימושיות זמני היום

21.10.2026. הנחיות המשתמש החדשות מחליפות את ברירת המחדל של הגדרת מיקום ידנית במסמך המקורי.

- ברירת מחדל חדשה: תל אביב–יפו (32.0853,34.7818; Asia/Jerusalem), עם הצגת זמנים פעילה. מיקום או כיבוי שנבחרו במפורש נשמרים בשדרוג. התקנות 134 ללא מיקום מקבלות תל אביב.
- כל ההעדפות נמצאות בתוך מסך ההעדפות הרגיל: הפעלה/כיבוי, הדגשת הבא, מיקום, בחירת זמנים וסדר, כניסת שבת וחג, מנהג הדלקת נרות ולוח ישראל/חו״ל. לחיצה על התאמה אישית בגיליון הפרטים מפנה לאותו סעיף במסך הרגיל.
- אין רשימת ערים במסך ההעדפות ואין תוצאות בחיפוש ריק. לחיצה על שינוי פותחת חיפוש ממוקד בתוך אותו מסך, ורק מהתו השני מוצגים יישובים תואמים.
- התאריך הלועזי כבוי כברירת מחדל. אפשר להפעילו בהעדפות; הוא מופיע לצד התאריך העברי, באותה שורה.
- תווית הזמן, השעה והאייקון נמצאים בשורה אחת. האייקון מימין לתווית ב־RTL. אייקוני קו מקוריים: זריחה, שקיעה, שמש, שעון, ירח ונרות. תאים מתרחבים כשצריך כדי לא לחתוך שמות ארוכים או טקסט מוגדל.
- אם השורה ניתנת לגלילה, שתי הפעמים הראשונות שבהן היא מופיעה כוללות תנועת היכרות של 36dp וחזרה. הספירה נשמרת. אין תנועה אחרי תחילת גרירה, בעת ניווט ידני בשורה, עם ביטול אנימציות במערכת או בזמן שימוש בחקירה במגע של קורא מסך.
- כניסת שבת/חג מוצגת ראשונה בימים הרלוונטיים, בלי להסיר את זמני המשתמש. ניתן לכבותה. לוח חגים מקומי מ־KosherJava מבחין בין ישראל וחו״ל; קיימת בחירה מפורשת לשינוי הלוח.
- ערב שבת קודם תמיד להדלקה אחרי צאת היום, גם כשיום שישי עצמו חג. בערב חג רגיל: הדלקה לפני השקיעה. בחג שמתחיל אחרי שבת או חג: נרות חג רק אחרי 8.5°, עם הסבר על אש קיימת; אין הצגה של זמן מוקדם כאילו מותר להדליק בו.
- מנהג הדלקת הנרות מוצג וניתן לשינוי. ברירות מחדל ליד נקודות היישוב: תל אביב 22 דקות, ירושלים 40, חיפה 30; במקומות אחרים 18. שינוי לוח או דקות מבטל תוצאה קודמת מיד. מועדים מוקדמים מוצגים בעיגול מעלה; מועדי הדלקה לפני שקיעה בעיגול מטה.

## מקורות

- תל אביב — לוח חב״ד (מציין 22 דקות): https://www.chabad.org/calendar/candlelighting_cdo/locationId/531/locationType/1/jewish/Candle-Lighting.htm
- כללי הדלקה, כולל חג אחרי שבת/חג: https://www.chabad.org/library/article_cdo/aid/87131/jewish/How-to-Light-Shabbat-Candles.htm
- לוחות חגים: KosherJava 2.5.0 JewishCalendar.hasCandleLighting / isAssurBemelacha.
- ההבחנה בין צאת יום חול לצאת שבת/חג: https://www.chabad.org/library/article_cdo/aid/3209349/jewish/About-Our-Zmanim-Calculations.htm

הנוסחאות ומנהגי המקום טעונים ביקורת הלכתית לפני הפצה. בדיקות התוכנה אינן אישור הלכתי. הבדיקות מתבצעות מקומית במחשב כדי שלא להחליף את האפליקציה האחרת בחזית המכשיר המחובר.

## תוצאות בדיקות סופיות

38 בדיקות ממוקדות עברו ללא כישלונות: חישובי זמנים ושבת/חג, שמירת העדפות, מסך ההעדפות וחיפוש יישוב, RTL ברוחב 320/360/390, תאריך לועזי אופציונלי, גלילה ורמז מוגבל לשתי כניסות, היום יום ונרמול גרשיים. הבנייה של APK 135 הצליחה.

בהרצה הרחבה נמצאו 3 כישלונות נוספים ב־ReaderFontSelectionTest: שתי בדיקות מחפשות גרש/גרשיים טיפוגרפיים במקום התווים הישרים לאחר הנרמול, ובדיקת שולי עמוד מודפס מצפה ל־236 ומקבלת 216. הן לא תוקנו במסגרת שינוי השימושיות. Lint ממשיך לדווח על NewApi ב־themes.xml ועל UnsafeOptInUsageError ב־PrayerPlaybackService.kt, כמו בבדיקה הקודמת.

## Release approval follow-up

Both lint errors were fixed: the navigation-bar attribute now lives in API 27 resources, and onGetSession explicitly opts into the Media3 API. lintRelease reports zero errors.

Reader tests now locate plain quote characters, compare absolute baselines (rather than per-word font metrics), and exclude source-indented lines from the flush-margin check. Release UI tests register their in-memory Compose host. No printed-layout production code was changed to satisfy these checks.

All 53 release checks passed across the broad run (52) and the corrected printed-page test rerun (1). The signed 135 bundle passed certificate, package, non-debuggable and bundletool validation.

Google Play upload and commit succeeded on 2026-10-21. A separate API read confirmed Alpha release 2.0 (135), version 135, status completed. Tester install availability / Google review status was not verified. Production was unchanged. Evidence: play-status-after-135.json.
