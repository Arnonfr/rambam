# שחזור ובדיקת Michael

כל הפקודות מתוך תיקיית `קוד`. בהתאם להנחיות המשתמש, פקודות shell מתחילות ב־`rtk`. אם כלי הבנייה אינם זמינים בסביבת הסוכן, יש לאתר את סביבת Android המתאימה; אין לדווח שעברו בדיקות שלא הורצו.

## משקלי הקול לבדיקת אינטגרציה

יש להוריד את [המשקלים המקובעים](https://huggingface.co/Phonikud/phonikud-tts-checkpoints/resolve/ff64ba389a3833c49a85c58fc51a7060a6984b87/michael.onnx) ולשמור בשם `voice.onnx` בתיקייה מקומית לבדיקות.

- גודל צפוי: `63516050` בתים.
- SHA-256: `d2824d46ecd7ca8a206686d818d3178effe4661ce78bdb4e754eed32fe604320`.
- בבנייה הקודמת תיקיית הבדיקה הייתה `/tmp/baderech-michael-model`. זו תיקייה זמנית; אין להניח שתשרוד או תימצא במחשב אחר.
- אין צורך להוריד Nakdimon לבדיקה הנוכחית. אין להשתמש במשקלי SASpeech הישנים תחת `/tmp/baderech-hebrew-model` בתור Michael.

סביבת הבנייה ששימשה לאימות גרסה 138:

```text
JAVA_HOME=/Users/hyh/.local/jdks/jdk-21.0.10+7/Contents/Home
ANDROID_HOME=/Users/hyh/Library/Android/sdk
Gradle=/Users/hyh/.gradle/wrapper/dists/gradle-9.3.1-bin/23ovyewtku6u96viwx3xl3oks/gradle-9.3.1/bin/gradle
```

יש לוודא שהנתיבים קיימים בסביבה הנוכחית. בהעברת השיחה לסביבת ביצוע אחרת `rtk` לא היה ב־PATH; קובצי המסירה נוצרו דרך ממשק הקבצים, ולא בוצעה בשל כך הרצה חדשה של הבדיקות.

## בדיקות ממוקדות

החלף את נתיב המשקלים, הפלט ונתיב Gradle לפי הסביבה. חובה להעביר `--tests` מיד לאחר משימת הבדיקה, לפני משימות build אחרות.

```sh
rtk proxy env \
  PIPER_TEST_MODELS=/path/to/verified-michael-weights \
  PIPER_TEST_OUTPUT=/path/to/audio-output \
  gradle :app:testDebugUnitTest \
  --tests com.example.PiperModelIntegrationTest \
  --tests com.example.MichaelPronunciationTest \
  --tests com.example.DailySpeechTest \
  --tests com.example.DailySpeechControlsTest \
  --tests com.example.RambamAudioVisibilityTest \
  --tests com.example.TanyaPlaybackControlsTest \
  :app:assembleDebug -Proborazzi.test.record=true --console=plain
```

`PiperModelIntegrationTest` משתמש במשקלי ONNX אמיתיים. ללא `PIPER_TEST_MODELS` הוא **מדלג** על הבדיקה — build ירוק אינו ראיה שבוצעה הפקת שמע. יש לוודא `skipped=0` ב־JUnit XML.

ליצירת הקראה של טקסט אחר לצורך בדיקה ניתן להעביר `PIPER_COMPARE_TEXT_FILE=/path/to/text.txt`. זה מנגנון של בדיקת האינטגרציה, לא יכולת טקסט חופשי המובטחת למשתמשי האפליקציה.

תוצאות: `app/build/test-results/testDebugUnitTest/`. צילומי מסך: `app/build/review/`. לאחר שהמקורות יציבים יש להריץ `:app:lintDebug` ולבדוק XML לשגיאות `Error`/`Fatal`: בפרויקט `abortOnError=false`, ולכן `BUILD SUCCESSFUL` לבדו אינו מספיק. אין לערוך Kotlin בזמן ניתוח lint.

## בנייה מחדש של אינדקס ההגייה

נדרש רק כשמשנים מקור טקסט/מילון/עיבוד הגייה. אין צורך להכין מחדש רק כדי להשתמש בקול Michael הקיים.

הכלי: `tools/build_michael_pronunciation.py`.

סביבה מבודדת ששימשה להכנה:

- Python ‏3.12.
- `phonikud==0.4.1`, `phonikud-onnx==1.0.6`, ‏ONNX Runtime ‏1.31.0.
- מודל ההכנה `phonikud-1.0.int8.onnx`, כ־308MB, ממאגר `thewh1teagle/phonikud-onnx` ב־Hugging Face. הוא אינו מודל הקול ואין להוסיף אותו להורדה במכשיר.
- ה־tokenizer שבו משתמשת הספרייה: `dicta-il/dictabert-large-char-menaked`.

```sh
rtk proxy python3 tools/build_michael_pronunciation.py \
  --model /path/to/phonikud-1.0.int8.onnx \
  --cache /path/to/pronunciation-hints.jsonl \
  --seed docs/agent-handoff/audio/reference
```

תיקיית ה־seed מכילה את `text-pointed.txt` ו־`predicted-pronunciation.txt` מהדוגמה שנבחרה. אין להשמיט אותה אם רוצים לשמור על התאמת הדוגמה. המטמון מאפשר המשך הכנה בלי לחשב מחדש את כל רמזי המודל. בהכנה הראשונה חושבו כ־51 אלף מילות בסיס; הפעולה ארכה כמה דקות.

הפלט הוא `app/src/main/assets/speech/michael-pronunciation.json`. יש לשמור אותו כ־**JSON רגיל** ולקרוא אותו בהתאם בקוד. ניסוי עם סיומת `.json.gz` נכשל משום ש־Android packaging הסיר את הסיומת ופרס את התוכן; כבר תוקן. אין להחזיר חיפוש של `.json.gz` בתוך AssetManager.

אחרי שינוי אינדקס:

1. הרץ את בדיקת הכיסוי ואת בדיקת הדוגמה שנבחרה.
2. בדוק הגייה של מילים ששונו בהקשר, והפק שמע אמיתי.
3. שנה את גרסת מפתח הקול/הגייה ב־`PiperHebrewSpeech.initialize()` אם שמע שמור צריך להיווצר מחדש.
4. אמת שהנכס הארוז ב־APK תואם לנכס בקוד. גרסה 138 אומתה כך.

אין להפוך שינוי במקור הספר לתחליף לתיקון פונטי. אין לעדכן fixture כדי להשתיק כישלון בלי לבדוק מדוע ההגייה השתנתה.

## בדיקות פיזיות שנותרו

בגרסה 138 לא היה מכשיר מחובר. יש לבדוק בטלפון אמיתי:

- הורדה ראשונה, הפרעת רשת באמצע, ניסיון חוזר, ומצב ללא אינטרנט אחרי ההורדה.
- שיעור מלא של שלושה פרקים, מעבר בין הלכות ארוכות המפוצלות לכמה מקטעים, והשמעת הקטע הראשון לפני שהכול הוכן.
- מסך נעול/רקע, Bluetooth ורכב, אוזניות, שיחת טלפון ואובדן audio focus.
- השהיה בזמן הכנה, הסתרת הנגן, שינוי יום והמשך אחרי פתיחה מחדש.
- מעבר מתניא/תפילה לרמב״ם ובחזרה: מקור יחיד נשמע וההכנה הישנה מבוטלת.
- ביצועים, זיכרון וחימום על מכשיר בינוני, וכן דיוק ונוחות האזנה של פרקים שונים.

## הפצות ואבטחת קבצים

נקודת הייחוס האחרונה לאודיו: `הפצות/138-Michael` בתיקיית העבודה שמעל `קוד`. receipt הועתק לכאן. קובץ ההתקנה הוא debug של Firebase עם מזהה חבילה נפרד; אין להתייחס אליו כגרסת החנות.

- Firebase: `tools/distribute_firebase.py` ו־`docs/firebase-distribution.md`.
- מסלול הבודקים בחנות: `tools/distribute_google_play.py`, `fastlane/Fastfile`, ‏`docs/google-play-distribution.md`.
- אין לכלול סיסמאות, keystore, חשבון שירות או URL הורדה זמני חתום במסירה/בלוגים.
- הצלחת העלאה למסלול Play אינה הוכחה שסקירת Google הסתיימה. גרסה 138 של Michael הופצה ל־Firebase בלבד לפי קבלת ההפצה.
