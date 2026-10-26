# ווידג׳ט ״היום יום״: הנחיות שילוב לגרסה הבאה

ווידג׳ט למסך הבית של Android. בחלק העליון מוצג פתגם ״היום יום״ של היום, בלי השיעורים, ולחיצה עליו פותחת את השיעור המלא. בתחתית יש כפתורי טקסט בלי מסגרת: **חומש | תהילים | תניא | רמב״ם**. כפתור רמב״ם פותח את המסלול שהמשתמש בחר: פרק אחד, שלושה פרקים או ספר המצוות.

כל הקבצים כאן חדשים. אין בתיקייה הזאת שינוי בקבצים קיימים. השינויים בקבצים קיימים מתוארים בשלבים 2–4, וכולם יחד כ־40 שורות.

## מה יש בתיקייה

| קובץ | תפקיד |
|---|---|
| `widget/WidgetModels.kt` | מודלים: שיעור, ערכת צבעים (שחור/לבן), השיעור המרכזי. צבעי השיעורים זהים לרצועות במסך הבית (`HomeNewViews.kt`). |
| `widget/HayomYomHebrew.kt` | בווידג׳ט ״היום יום״ מוצג בעברית בלבד: המקור העברי ותרגום ״ללשון הקודש״, בלי הפסקאות ביידיש. הטקסט המלא נשאר בקורא באפליקציה. |
| `widget/WidgetTextFit.kt` | חישוב כמה טקסט נכנס בגודל הנוכחי. אם הטקסט נכנס, הוא מוצג במלואו. אחרת מוצגת ההתחלה ו״הצג עוד״. ככל שמגדילים, מוצג יותר. |
| `widget/WidgetContentLoader.kt` | טוען את טקסט היום מהמאגרים הקיימים (היום יום, תניא, חומש, תהילים, רמב״ם, ספר המצוות). את היום מחשב בדיוק כמו ה־ViewModel: שעון ישראל, חצות או שקיעה לפי ההגדרה. |
| `widget/DailyStudyWidget.kt` | הווידג׳ט עצמו (Jetpack Glance). |
| `widget/DailyStudyWidgetReceiver.kt` | Receiver, רענון כל הווידג׳טים, ושעון שמעיר את הווידג׳ט במעבר יום (חצות או שקיעה). |
| `widget/DailyStudyWidgetConfigureActivity.kt` | מסך הגדרות: שחור או לבן, השיעור המרכזי, ותצוגה מקדימה חיה. |
| `widget/WidgetConfigStore.kt` | ההגדרות של כל ווידג׳ט בנפרד (DataStore של Glance). |
| `widget/WidgetLaunch.kt` | Intent מהווידג׳ט לאפליקציה, ו־`openFromWidget()` שפותח את השיעור הנכון. משתמש רק ב־API הציבורי של `DailyRambamViewModel`. |
| `res/xml/daily_study_widget_info.xml` | הגדרת הווידג׳ט: ‎4×2 כברירת מחדל, ניתן להקטין ל־‎3×2 ולהגדיל. |
| `res/layout/daily_study_widget_preview.xml` | תצוגה מקדימה בבוחר הווידג׳טים (Android 12 ומעלה). |
| `res/layout*/widget_vilna_text_*.xml` | TextView בגופן Romm Vilna (`layout-v26`), ובגופן מערכת ב־Android 7. |
| `res/drawable/…`, `res/drawable-nodpi/…` | רקע שחור או לבן, ותמונת תצוגה מקדימה לגרסאות ישנות. |
| `test/…/DailyStudyWidgetLogicTest.kt` | 10 בדיקות לוגיקה. |
| `test/…/DailyStudyWidgetRenderTest.kt` | רינדור של ה־RemoteViews האמיתיים לתמונות, בכל הגדלים ובשתי ערכות הצבע, וגם של מסך ההגדרות. |
| `design/` | התמונות מהרינדור. אלה העיצוב בפועל. |

## שלבי השילוב

### 1. העתקת הקבצים

```bash
cd קוד
rsync -a widget-integration/app/ app/
```

### 2. תלות Glance

ב־`gradle/libs.versions.toml`:

```toml
[versions]
glance = "1.1.1"

[libraries]
androidx-glance-appwidget = { group = "androidx.glance", name = "glance-appwidget", version.ref = "glance" }
```

ב־`app/build.gradle.kts`, ליד `datastore.preferences`:

```kotlin
implementation(libs.androidx.glance.appwidget)
```

### 3. Manifest

בתוך `<application>`:

```xml
<receiver
    android:name=".widget.DailyStudyWidgetReceiver"
    android:exported="true"
    android:label="@string/daily_study_widget_label">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
        <action android:name="android.intent.action.TIME_SET" />
        <action android:name="android.intent.action.TIMEZONE_CHANGED" />
    </intent-filter>
    <meta-data
        android:name="android.appwidget.provider"
        android:resource="@xml/daily_study_widget_info" />
</receiver>
<activity
    android:name=".widget.DailyStudyWidgetConfigureActivity"
    android:exported="true"
    android:excludeFromRecents="true"
    android:taskAffinity=""
    android:theme="@style/Theme.MyApplication">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_CONFIGURE" />
    </intent-filter>
</activity>
```

לא נדרשות הרשאות חדשות. השעון הוא `AlarmManager.set(RTC)` רגיל: לא מדויק ולא מעיר את המכשיר. הוא מופעל ברגע שהמסך נדלק, וזה בדיוק הרגע שבו רואים את הווידג׳ט.

### 4. MainActivity

```kotlin
import com.example.widget.openFromWidget

class MainActivity : ComponentActivity() {
    private val widgetRequest = MutableStateFlow<com.example.widget.WidgetStudy?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) widgetRequest.value = com.example.widget.WidgetLaunch.consume(intent)
        setContent {
            // ... קיים: viewModel, uiState, prayerId, prayer ...

            val pendingWidgetStudy by widgetRequest.collectAsStateWithLifecycle()
            LaunchedEffect(pendingWidgetStudy) {
                val study = pendingWidgetStudy ?: return@LaunchedEffect
                prayerId = null                       // סוגר תפילה פתוחה
                viewModel.openFromWidget(study)
                widgetRequest.value = null
            }
            // ... המשך קיים ...
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        com.example.widget.WidgetLaunch.consume(intent)?.let { widgetRequest.value = it }
    }

    override fun onStop() {
        super.onStop()
        // מסלול רמב״ם, שיעורים מוצגים, חצות/שקיעה או ניקוד ייתכן שהשתנו
        com.example.widget.DailyStudyWidgetUpdater.requestRefresh(this)
    }
}
```

את הבלוק של `LaunchedEffect` יש לשים אחרי ההגדרה של `prayerId`. `openFromWidget` מחכה שהטעינה הראשונה תסתיים וחוזר ליום של היום. אחר כך הוא סוגר כל קורא פתוח ופותח את השיעור. ברמב״ם הוא משתמש ב־`openRambamTrack(track)`, כמו הרצועה במסך הבית.

### 5. תמונת תצוגה מקדימה לגרסאות ישנות

`daily_study_widget_preview_image.png` נמצא ב־`res/drawable-nodpi/`. הוא נלקח מהרינדור, ומשמש בבוחר הווידג׳טים עד Android 11. אין צורך בפעולה נוספת.

### 6. בדיקה

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.widget.*"
```

התמונות נשמרות ב־`app/build/outputs/roborazzi/widget/`.

בדיקה ידנית במכשיר:
1. לחיצה ארוכה על מסך הבית, ווידג׳טים, ״היום יום״. ב־Android 12 ומעלה הווידג׳ט מתווסף מיד עם ברירת המחדל: שחור והיום יום. בגרסאות ישנות נפתח קודם מסך ההגדרות.
2. לחיצה על הטקסט או על ״הצג עוד״ פותחת את קורא ״היום יום״ של היום. כל כפתור פותח את הקורא שלו.
3. במסלול ״3 פרקים״ כפתור רמב״ם פותח שלושה פרקים. במסלול ספר המצוות הכפתור תכלת ופותח את ספר המצוות.
4. בגודל ‎4×2 מוצגות כארבע שורות ו״הצג עוד״. הגדלה מציגה יותר, ומ־‎4×5 בערך רוב ימי ״היום יום״ מוצגים במלואם. יש לוודא שהטקסט לא נכנס לשורת הכפתורים בגופן ובגודל הגופן של המכשיר.
5. לחיצה ארוכה על הווידג׳ט, ״הגדרות״ (Android 12 ומעלה): החלפה ללבן או לשיעור מרכזי אחר.
6. מעבר יום: במצב ״שקיעה״, אחרי השקיעה, הווידג׳ט עובר לתאריך הבא.

## החלטות עיצוב

- **מבנה:** מלבן מרובע לגמרי, רקע שטוח שחור (`#151517`) או לבן, עם מסגרת דקה. הווידג׳ט לא משתמש ב־`appWidgetBackground()`, כדי שמשגרי Android 12 ומעלה לא יעגלו את הפינות. בפינה התחתונה יש זוהר חלש מאוד בצבע השיעור המרכזי.
- **גופן:** Romm Vilna, הגופן של תצוגת דף התניא באפליקציה. Glance לא מאפשר גופן של האפליקציה, ולכן כל טקסט הוא TextView מ־XML שמוטמע דרך `AndroidRemoteViews` (`VilnaText` ב־`DailyStudyWidget.kt`). בגופן הזה המקפים – ו־— ריקים, ולכן הם מוחלפים ב־-.
- **כותרת:** שם השיעור בצבע שלו, ואחריו ״· ג׳ כסלו״ באפור. אין כפתור פתיחה נפרד: כל אזור הטקסט פותח את השיעור, ו״הצג עוד״ מופיע רק כשהטקסט נחתך.
- **כפתורים:** תמיד בגובה 46dp, ורק הטקסט משתנה. בשחור הצבעים זהים לאפליקציה. בלבן הגוונים מועמקים כדי לשמור על ניגודיות 4.5:1 (צהוב החומש לא קריא על לבן). הצבעים מוגדרים ב־`WidgetPalette`.
- **שיעור מרכזי אחר:** אם נבחר למשל תניא, הכפתור ״תניא״ בתחתית מתחלף ב־״היום יום״, כדי שאף שיעור לא יופיע פעמיים.
- **התאמת טקסט:** RemoteViews לא מודד טקסט, אז `WidgetTextFit` מעריך לפי גודל הווידג׳ט וגודל הגופן במערכת (`fontScale`). הקבועים כוילו על הרינדור בגופן Vilna: שורה היא 1.08em ותו 0.37em בממוצע. ההערכה מעט שמרנית, כך שהטקסט לא נדחס לתוך הכפתורים.
- **בלי גלילה:** נשקלה רשימה גלילה (LazyColumn) לווידג׳ט מוגדל, אבל אי אפשר לבדוק אותה בסביבת הבדיקות, ולא נבדקה במכשיר יחד עם הטקסט המוטמע. לכן ווידג׳ט מוגדל מציג יותר טקסט בלי גלילה, וטקסט ארוך מדי נפתח ב״הצג עוד״.
- **היום יום בעברית:** ברשומות של חב״דפדיה המקור כתוב חלקו ביידיש, ואחריו תרגום. התרגום מופיע אחרי `----` ״תרגום ללשון הקודש:״ (137 ימים), או כפסקאות נפרדות. הווידג׳ט לוקח רק את העברית. ב־3 ימים אין תרגום, ושם מוצג המקור. הקורא באפליקציה עדיין מציג הכול.
- **ניקוד:** לפי ההגדרה ״הצג ניקוד״ באפליקציה.
- **כיוון:** Row ב־Glance הולך לפי שפת המשגר. לכן הסדר מימין לשמאל נשמר גם במכשיר באנגלית: חומש מימין ורמב״ם משמאל.

## מה נבדק ומה לא

- **נבדק:** קומפילציה ובניית APK מלאה עם השילוב. 10 בדיקות לוגיקה ו־4 בדיקות רינדור (Robolectric + Roborazzi) עוברות. התמונות ב־`design/` הן הרינדור האמיתי של ה־RemoteViews עם נתוני היום.
- **לא נבדק במכשיר:** הוספה למסך הבית, הלחיצות שפותחות את הקוראים (`openFromWidget`), מסך ההגדרות מתוך המשגר, ומעבר יום. אמולטור מקביל לא היה יציב במחשב הזה. יש לבצע את רשימת הבדיקה בשלב 6 במכשיר או באמולטור לפני הפצה.

## מגבלות ידועות

- **תניא** נטען מהמטמון או מספריא, ואם אין רשת מוצגת הודעה. **רמב״ם** כשיעור מרכזי דורש שהאפליקציה נפתחה לפחות פעם אחת, כדי שמסד הנתונים יהיה מוכן.
- ב־Android 7 (API 24–25) הטקסט מוצג בגופן מערכת, כי גופני XML נתמכים מ־Android 8.
- Glance 1.1.1 הוא הגרסה היציבה שנבדקה. אם עוברים ל־1.2, אפשר להוסיף תצוגה מקדימה מיוצרת (Generated Previews, Android 15).
