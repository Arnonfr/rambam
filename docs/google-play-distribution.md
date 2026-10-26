# הפצה לבודקי Google Play דרך CLI

הוגדר fastlane 2.240.1 עם Ruby 3.4.11 ו־Bundler. הגרסאות ננעלות בקובצי `mise.toml`, `Gemfile` ו־`Gemfile.lock`. פקודת `fastlane` מותקנת גם ב־`~/.local/bin`.

האפליקציה היא `com.aistudio.dailyrambam.kzjvw`, בחשבון Frielabs מספר `6556694888792594977`. מסלול הבדיקות הסגור הקיים נקרא **Alpha**, ושמו ב־API הוא `alpha`. בהקמה אומתה גרסה 125 במסלול הזה, וגרסה 114 במסלול `internal`.

חשבון השירות `play-test-distributor@daily-rambam-pilot-20261001.iam.gserviceaccount.com` מוגבל לאפליקציה זו עם שתי הרשאות: הצגת מידע לא פיננסי והפצה למסלולי בדיקה. אין לו הרשאת הפצה לייצור. Android Publisher API הופעל בפרויקט `daily-rambam-pilot-20261001`.

מפתח השירות נשמר מחוץ לתיקיית Git, ב־`חתימה-פרטי/google-play-service-account.json`, עם הרשאות קובץ 0600. פרטי מפתח החתימה נמצאים ב־`חתימה-פרטי/signing-config.json`; סיסמת מפתח ההעלאה נשארת במחזיק המפתחות של macOS, בשירות `codex-rambam-upload-key`. אין צורך להעתיק סיסמאות לקובץ או לפקודה. בנייה חתומה מתבצעת ללא מטמון הגדרות Gradle.

מתוך תיקיית `קוד`, בדיקת מצב בלבד:

```sh
rtk proxy python3 tools/distribute_google_play.py --status
```

בנייה חתומה והפצה לבודקי Alpha:

```sh
rtk proxy python3 tools/distribute_google_play.py --notes /absolute/path/release-notes.txt
```

הפצת חבילה חתומה שכבר נבנתה:

```sh
rtk proxy python3 tools/distribute_google_play.py --aab /absolute/path/release.aab --notes /absolute/path/release-notes.txt
```

הכלי מאמת את מבנה ה־AAB, חתימת החבילה, טביעת האצבע של מפתח ההעלאה הקיים, שם החבילה ומספר הגרסה. Fastlane מעלה את החבילה והערות הגרסה למסלול `alpha` בלבד. הגדרות המסלול ורשימת הבודקים נשארות כפי שהוגדרו בחנות.

ההגשה מבקשת שליחה לבדיקה. שגיאת הגשה נעצרת ומוצגת; הכלי אינו מסתיר אותה באמצעות שמירה ללא שליחה לבדיקה. הצלחת API וסטטוס `completed` של מסלול אינם מוכיחים שהגרסה זמינה כבר לבודקים: יש לבדוק את מצב הסקירה של Google בסיכום השינויים לפרסום. הפרסום המנוהל כרגע כבוי.

להקמת מחשב נוסף נדרשים המפתח הפרטי והרשאות החנות, Ruby/Bundler, Android SDK, JDK 21, Gradle 9.3.1 ו־Google bundletool. אין לכלול מפתחות ב־Git או בקובצי הפצה. מפתח מנהל המערכת הזמני ששימש להגדרה בוטל ונמחק אחרי ההקמה.

תיעוד ראשי: [fastlane supply](https://docs.fastlane.tools/actions/upload_to_play_store/), [Google Play Developer API](https://developers.google.com/android-publisher/getting_started).
