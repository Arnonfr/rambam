# מדידת שימוש באודיו — גרסה 140

23.10.2026. המדידה הוטמעה בקוד ו־Google Analytics הופעל דרך Firebase Management API בחשבון Analytics הקיים. אין צורך להפעיל את השירות ידנית. המדידה מתחילה רק בגרסה הכוללת אותה; אין נתוני האזנה היסטוריים.

החל מגרסה 142 נוספו גם הקלטות רמב״ם. הן נספרות באותם אירועי רמב״ם עם `engine=recording`; הקראת Michael נשארת `engine=michael`. מסלולי פרק אחד ושלושה נשמרים. אין כפל ספירה מול Michael, כי החלפת מקור עוצרת את הנגן הקודם. ראו [הטמעה ואימות](RECORDINGS-142.md).

## איך רואים

הדשבורד הוגדר ופורסם בפועל ב־23.10.2026: [בדרך — שימוש באודיו](https://analytics.google.com/analytics/web/#/a69658038p560518590/reports/builder/16188969249?params=_u..nav%3Dmaui%26_u.dateOption%3Dtoday%26_u.comparisonOption%3Ddisabled). נמצא בתפריט **דוחות ← מרכזי בקרה מותאמים אישית ← בדרך — שימוש באודיו**. כולל משתמשים פעילים באפליקציה, סך דקות האזנה בפועל וטבלת משתמשים/מספר אירועים לכל שלב ולימוד (מסנן `audio_`, עד 100 שורות, ללא שורת סיכום כפולה). כרטיס הדקות משתמש ב־Event value ומסנן `audio_listen_time` בלבד; אינו מחבר את אירועי המסלולים. הדוח נשמר עם תיאור בעברית. הקישור מציג היום; לניתוח שבועי יש לבחור טווח מתאים בבורר התאריכים.

**הסיבה לדוח הריק שנבדקה:** קישורים ללא מזהה החשבון `a69658038` נפתחו בנכס אחר. בנוסף, טווח ברירת המחדל של 28 ימים הסתיים אתמול, לפני תחילת המדידה. עברנו לנכס הנכון ובחרנו היום. הדוחות המעובדים עדיין לא הציגו נתונים בזמן ההגדרה, אבל זמן אמת הציג בפועל `audio_listen_time`, `audio_rambam_one_minutes`, `audio_engaged`, `audio_available` ו־`audio_panel_open`. המשתמש אישר שהפעלתו בגרסה 140 מ־Firebase. לא ניתן לייחס את כל המשתמשים בחלון זמן האמת למשתמשים אמיתיים: נערכו גם בדיקות אמולטור.

1. פותחים [דוח האירועים ב־Google Analytics](https://analytics.google.com/analytics/web/#/a69658038p560518590/reports/engagement-events), או [Firebase Analytics](https://console.firebase.google.com/project/daily-rambam-pilot-20261001/analytics/app/events).
2. בוחרים טווח של 7 או 30 ימים, ומסננים לפי App / Stream: יישום החנות `Baderech` או יישום הבודקים `Tanya Page Pilot`. אלה שני זרמי נתונים באותו נכס. בדיקות פיתוח ב־Debug mode אינן מדגם משתמשים.
3. מחפשים `audio_`. בטבלה חשוב להשוות **Total users**, לא רק Event count: אדם יכול ללחוץ כמה פעמים. הספירה היא מזהי התקנה פסאודונימיים, לא בהכרח אנשים ייחודיים.
4. סך דקות האזנה נמצא ב־**Event value** של אירוע `audio_listen_time` או של אירוע `..._minutes` למסלול מסוים. `value` נשלח כבר בדקות. אין לחבר את האירוע הכללי ואת אירועי המסלולים, משום שאלה שתי תצוגות של אותם נתונים.

לבדיקה מיידית: [Realtime](https://analytics.google.com/analytics/web/#/a69658038p560518590/realtime/overview?params=_u..nav%3Dmaui) ו־Firebase ← Analytics ← DebugView. קישור זמן אמת אומת בדפדפן ונשאר פתוח בסיום. דוחות רגילים אינם מתעדכנים מיד; לתת להם 24–48 שעות מתחילת האיסוף, לפי [תיעוד זמני העיבוד של Google](https://support.google.com/analytics/answer/11198161?hl=en). ספירת אירועי `audio_listen_time` בזמן אמת אינה מספר הדקות; דקות מוצגות בכרטיס הייעודי לאחר עיבוד הנתונים.

## מה לבדוק בכל שבוע

| שאלה | אירוע כללי | דוגמה: רמב״ם פרק אחד |
| --- | --- | --- |
| כמה פתחו מסך שמציע אודיו? | `audio_available` | `audio_rambam_one_available` |
| כמה ניסו להפעיל? | `audio_play_requested` | `audio_rambam_one_requested` |
| כמה הגיעו לניגון בפועל? | `audio_play_started` | `audio_rambam_one_started` |
| כמה הגיעו ל־30 שניות בהאזנה? | `audio_engaged` | `audio_rambam_one_engaged` |
| כמה הגיעו לחמש דקות? | `audio_five_minutes` | `audio_rambam_one_five_minutes` |
| כמה דקות שמע נוגנו בפועל? | `audio_listen_time` | `audio_rambam_one_minutes` |
| כמה האזינו לפחות 30 שניות ביום, גם בכמה האזנות קצרות? | `audio_listening_day` | `audio_rambam_one_listening_day` |
| כמה חזרו ביום נוסף, עד שבוע מהיום הקודם עם 30 שניות האזנה? | `audio_return_7d` | `audio_rambam_one_return_7d` |
| כמה נתקלו בכשל הכנה או נגן? | `audio_error` | `audio_rambam_one_error` |

אותו מבנה קיים ל־`rambam_three`, `tanya`, `prayer`. האירועים הנפרדים מאפשרים לקרוא דוח שימוש בסיסי בלי להגדיר ממדים מותאמים. גם שימוש בכמה מסלולים אינו הופך התקנה אחת לכמה משתמשים באירוע הכללי.

חישובים פשוטים: משתמשי `requested` חלקי משתמשי `available` = ניסיון שימוש; משתמשי `started` חלקי משתמשי `requested` = הגעה לשמע; משתמשי `engaged` חלקי משתמשי `started` = עומק ראשוני. אלה יחסים בתקופה שנבחרה, לא משפך מובטח ברמת סשן. לזמן הכנה ולתקלות יש משמעות לפני שמסיקים שאין ביקוש.

`return_7d` הוא סימן לחזרה מהאזנה משמעותית קודמת, לא retention של קבוצת מצטרפים עם מכנה שהבשיל. למדידת retention מדויק של 7 ימים מההאזנה הראשונה יש לבנות Cohort exploration / ייצוא ולהמתין שבעה ימים מלאים. לא להציג את היחס הפשוט כאותו מדד.

## מה שולחים ואיך סופרים

- השירות מחזיק את מדידת Media3, גם כשהמסך אינו פתוח. רק `isPlaying` של מקור האודיו הנוכחי מוסיף זמן; ההכנה, הורדת Michael, buffering והשהיה אינם מוסיפים זמן.
- שעון מונוטוני מודד זמן אמיתי. שינוי מהירות או קפיצה של שעה בפס הזמן אינם מוסיפים שעה לדוח. מעבר הלכה אינו מאפס האזנה לוגית.
- תוספת זמן נשלחת כל כ־30 שניות וגם בהשהיה, החלפת מקור וסגירה. זמן ממתין נשמר מקומית בכל דגימת שירות ומוחזר אחרי סגירת תהליך. אין הבטחת exactly-once מול SDK: תיתכן אבדה/כפילות קטנה בקריסה בדיוק בזמן מסירה. SDK מטפל בתור השליחה שלו; הדוח אינו מערכת חיוב.
- `audio_queue_ended` פירושו שהנגן הגיע לסוף התור המוכן. **זו אינה הוכחה שכל התוכן הושמע**, משום שאפשר לדלג.
- `audio_available` פעם למסלול ליום, בהופעת מסך הקריאה בחזית. זהו מכנה של גילוי אפשרות האודיו, ולא הבטחה שההקלטה אכן קיימת לכל יום/מורה. חומש אינו נספר כיכולת אודיו קיימת.
- `audio_panel_open` במעבר מהסתרת הנגן להצגתו; `audio_voice_download` כולל begin / success / failed / cancelled ו־duration_ms.
- פרמטרים: `content_type`, `engine`, `schema_version`, `build_type`; בהפעלה `voice_ready` (נוכחות קובץ בגודל הנכון, בדיקת תקינות מלאה בהמשך) ו־`startup_ms`; בזמן `listened_ms` ו־`value` בדקות; בשגיאה `reason` מקוד קבוע. לא נשלחים מזהי שיעור/סשן, טקסט, כתובות קבצים או הודעות חריגה.
- חזרה יומית מחושבת לפי יום לועזי באזור הזמן של המכשיר. מקטע דיווח שחוצה חצות מיוחס ליום המסירה, ויש לקחת זאת בחשבון סמוך לחצות.

## פרטיות

בהעדפות הרגילות ← פרטיות ← **שיתוף נתוני שימוש לשיפור האפליקציה**. ברירת המחדל פעילה; כיבוי מפסיק את Analytics, מסיר נתוני SDK מקומיים ומנקה צבירה מקומית. הוא אינו מוחק דוחות שכבר התקבלו בשרת. אירועי השימוש אינם כוללים שם, דוא״ל, טקסט לימוד או מיקום. Google Analytics משתמש במזהה התקנה פסאודונימי ומאפייני מכשיר אוטומטיים, כך שאין לתאר את האיסוף כמידע אנונימי לחלוטין. איסוף Advertising ID ושימוש לפרסום מנוטרלים.

אם רוצים לנתח את הפרמטרים הכלליים בדוחות, Admin ← Custom definitions: ממדים ברמת Event ל־content_type, engine, build_type, voice_ready, reason, stage; מדדים מספריים ל־listened_ms, startup_ms, duration_ms. אלה **לא נוצרו אוטומטית**: הרשאת ה־CLI הקיימת מאפשרת חיבור דרך Firebase, אך אינה מעניקה analytics.edit ל־Analytics Admin API. הדוח הבסיסי עם האירועים הנפרדים ו־Event value אינו תלוי בהגדרות האלה.

## חיבור והמשך עבודה

פרויקט Firebase: `daily-rambam-pilot-20261001`; חשבון Analytics: `69658038`; נכס: `560518590`.

| יישום | Firebase App ID | Stream ID |
| --- | --- | --- |
| חנות | `1:703198161750:android:cd9e6753f4b8abfdbbc4f7` | `16188541940` |
| בודקים | `1:703198161750:android:1e1d23970312aa68bbc4f7` | `16188537930` |

אישור ההפעלה: [analytics-activation.json](analytics-activation.json). קובצי google-services.json לכל גרסה נמצאים ב־app/src/debug וב־app/src/release; אין בהם מפתח פרטי. לפני פרסום ציבורי יש לשקף את האיסוף האמיתי במדיניות הפרטיות ובטופס Data safety של החנות.

לבדיקת מכשיר ייעודי: `adb -s SERIAL shell setprop debug.firebase.analytics.app com.aistudio.dailyrambam.kzjvw.debug`, ואז DebugView. בסיום `adb -s SERIAL shell setprop debug.firebase.analytics.app .none.`. אין לשנות זאת במכשיר שבודק אפליקציה אחרת.

מקורות: [Firebase events](https://firebase.google.com/docs/analytics/android/events), [DebugView](https://firebase.google.com/docs/analytics/debugview), [חיבור Analytics דרך API](https://firebase.google.com/docs/reference/firebase-management/rest/v1beta1/projects/addGoogleAnalytics).


## תוצאות אימות באמולטור

באפליקציה עצמה, גרסה 140: כניסה לרמב״ם, פתיחת הנגן ולחיצה על Play שלחו את האירועים לשירות Google Analytics, עם תשובת קבלה HTTP 204. ההורדה הראשונה נמדדה בנפרד ולא יצרה זמן האזנה. לאחר התאמת תאימות לאמולטור, ניגון Michael אמיתי התחיל תוך 4.4 שניות עם מודל שמור ושלח `audio_play_started`. נמדדו ונשלחו גם `audio_engaged`, `audio_listen_time` ו־`audio_listening_day`; הניגון והמדידה המשיכו כשהמסך כובה. בהשהיה של 29 שניות לא נוצר אירוע זמן נוסף. בכיבוי המדידה דרך ממשק ההעדפות לא נשלח שום אירוע אודיו נוסף במשך 49 שניות, וההעדפה נשמרה כ־false. הפעלה מחדש של המדידה משחזרת איסוף קדימה בלבד.

נחשפה קריסת SIGILL בפקודת ARM SME ‏`RDSVL` בתוך ONNX Runtime 1.31.0: האמולטור מצהיר על יכולת שאינה עובדת. `mlas.disable_kleidiai=1` הוגדר רק בחומרה `ranchu`/`goldfish`, בהתאם לאפשרות הרשמית של ONNX Runtime; טלפונים פיזיים שומרים את ההתנהגות הקודמת. אין קשר לקול מערכת ואין החלפת Michael.

לוגים מקומיים של SDK מוכיחים קבלה של חבילות בשרת, אך אינם תחליף לבדיקת הופעתן ב־DebugView/דוח המעובד. אימות דרך Analytics Data/Admin API של הדוח עצמו דורש הרשאה שאינה נכללת בכניסת Firebase CLI הנוכחית.


גרסה 140 הופצה בפועל: [קישור לבודקים](https://appdistribution.firebase.google.com/testerapps/1:703198161750:android:1e1d23970312aa68bbc4f7/releases/3f09i54nhvsio). [אישור ההפצה והבדיקות](../../firebase-release-140.json), [קבלת האימות המפורטת](validation-140.json), [ראיות אירועים נקיות ממזהים](analytics-emulator-evidence.json). לא הועלתה במסגרת עבודה זו גרסה חדשה ל־Google Play.
