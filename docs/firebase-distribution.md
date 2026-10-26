# הפצה לבודקים ללא דפדפן

Firebase CLI מותקן ב־`/Users/hyh/.local/bin/firebase` ומחובר לחשבון הקיים.
האפליקציה לבדיקות: `1:703198161750:android:1e1d23970312aa68bbc4f7` בפרויקט
`daily-rambam-pilot-20261001`. ההפצה ממשיכה לאותו בודק ששימש בגרסאות הקודמות.

לאחר בנייה ובדיקות, מריצים מתוך תיקיית הקוד:

```sh
rtk proxy python3 tools/distribute_firebase.py /path/to/build.apk /path/to/release-notes.txt
```

אפשר לבחור בודקים מפורשות באמצעות `--testers email1,email2`.
הפקודה מעלה את הקובץ, מוסיפה הערות גרסה, מפיצה לבודקים ומחזירה קישור התקנה.
היא משתמשת בהתחברות הקיימת ואינה פותחת דפדפן. אין צורך להתקין MCP לצורך ההפצה.

תיעוד רשמי: https://firebase.google.com/docs/app-distribution/android/distribute-cli

## הפצה אוטומטית מכל דחיפה

ה־workflow `.github/workflows/android-ci.yml` בונה ובודק כל דחיפה לכל ענף, ואז מפיץ
לפיירבייס לבודקים הקיימים. בקשות משיכה מריצות בדיקות ובנייה בלבד.
הפצה שכבר החלה אינה מבוטלת על ידי דחיפה חדשה. מספר הגרסה נבחר כמספר הבא אחרי
הגרסה האחרונה בפיירבייס, ולפחות `versionCode` שבמקור.

החתימה הקבועה נשמרת ב־GitHub Secret בשם `FIREBASE_DEBUG_KEYSTORE_BASE64`.
החיבור לגוגל משתמש ב־GitHub OIDC ו־Workload Identity Federation, דרך חשבון
`github-firebase-distribution@daily-rambam-pilot-20261001.iam.gserviceaccount.com`.
ההרשאה מוגבלת למזהה הריפו 1374162034, לאירועי push והפעלה ידנית, ולתפקיד
Firebase App Distribution Admin. אין מפתח JSON קבוע עבור ההפצה.

משתני הריפו: `FIREBASE_APP_ID`, `FIREBASE_TESTERS`,
`FIREBASE_WORKLOAD_IDENTITY_PROVIDER`, `FIREBASE_SERVICE_ACCOUNT`.
קישור התקנה יופיע בסיכום ההרצה רק לאחר אישור ההפצה, לצד APK ודוחות בדיקות.
קישורי הורדה חתומים וקובצי אישורים אינם מצורפים לתוצרים.

הערות ההפצה נלקחות מקובץ `docs/release-notes-N.txt` עם המספר הגבוה ביותר.
יש לעדכן אותו עם כל שינוי למשתמש. גם כשאין בקשה נפרדת להפצה, עבודת שינוי
אינה מסתיימת בקובץ מקומי: יש לדחוף ולעקוב אחרי הצלחת ההפצה בפיירבייס.
