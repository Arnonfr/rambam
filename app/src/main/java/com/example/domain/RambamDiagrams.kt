package com.example.domain

/** Conventional (database) numbering, NOT Mechon Mamre's subdivided numbering. */
data class RambamDiagram(
    val id: String,
    val sectionId: String,
    val chapter: Int,
    val halacha: Int,
    val title: String,
    val sourceUrl: String,
    val kind: Kind,
    val note: String = "תרשים עזר לפי ההלכה; אינו צילום כתב־יד ואינו בקנה מידה."
) {
    enum class Kind {
        TEFILLIN_MOLD, HEAD_ORDER, HAND_ORDER,
        OPEN_FIRST, OPEN_SECOND, CLOSED_FIRST, CLOSED_SECOND, CLOSED_THIRD,
        LAND_BOUNDARY, ALTAR_HEIGHT, ALTAR_FOUNDATION, MENORAH_ORIGINAL,
        LINTELS, HEICHAL, COURTYARD, SHOHAM, EPHOD, ROUND_BEAM
    }
}

object RambamDiagrams {
    private const val base = "https://mechon-mamre.org/i/"
    val all: List<RambamDiagram> = listOf(
        RambamDiagram("tefillin_mold", "tefillin", 3, 2, "אימום לתפילין של ראש", base + "2303.htm", RambamDiagram.Kind.TEFILLIN_MOLD),
        RambamDiagram("head_order", "tefillin", 3, 5, "סידור פרשיות של ראש", base + "2303.htm", RambamDiagram.Kind.HEAD_ORDER,
            "מבט מול המניח: שמאל התרשים הוא ימין המניח."),
        RambamDiagram("hand_order", "tefillin", 3, 6, "ארבע פרשיות על עור אחד", base + "2303.htm", RambamDiagram.Kind.HAND_ORDER),
        RambamDiagram("open_1", "tefillin", 8, 1, "פרשה פתוחה — צורה א׳", base + "2308.htm", RambamDiagram.Kind.OPEN_FIRST),
        RambamDiagram("open_2", "tefillin", 8, 1, "פרשה פתוחה — צורה ב׳", base + "2308.htm", RambamDiagram.Kind.OPEN_SECOND),
        RambamDiagram("closed_1", "tefillin", 8, 2, "פרשה סתומה — צורה א׳", base + "2308.htm", RambamDiagram.Kind.CLOSED_FIRST),
        RambamDiagram("closed_2", "tefillin", 8, 2, "פרשה סתומה — צורה ב׳", base + "2308.htm", RambamDiagram.Kind.CLOSED_SECOND),
        RambamDiagram("closed_3", "tefillin", 8, 2, "פרשה סתומה — צורה ג׳", base + "2308.htm", RambamDiagram.Kind.CLOSED_THIRD),
        RambamDiagram("land_boundary", "terumot", 1, 7, "גבול הארץ והאיים שבים", base + "7301.htm", RambamDiagram.Kind.LAND_BOUNDARY,
            "תרשים סכמטי של דין החוט המתוח; אינו מפה גאוגרפית או זיהוי מודרני של הגבולות."),
        RambamDiagram("altar_height", "beit_habechirah", 2, 9, "גובה המזבח וחוט הסיקרא", base + "8102.htm", RambamDiagram.Kind.ALTAR_HEIGHT),
        RambamDiagram("altar_foundation", "beit_habechirah", 2, 12, "יסוד המזבח והשיתין", base + "8102.htm", RambamDiagram.Kind.ALTAR_FOUNDATION),
        RambamDiagram("menorah", "beit_habechirah", 3, 10, "ציור המנורה בכתב יד הרמב״ם", base + "8103.htm", RambamDiagram.Kind.MENORAH_ORIGINAL,
            "צילום הציור מפירוש המשנה, מנחות ג׳ ז׳ — לא מכתב־יד של משנה תורה. מקור: Wikimedia Commons, נחלת הכלל."),
        RambamDiagram("lintels", "beit_habechirah", 4, 8, "חמש המלתריות מעל פתח האולם", base + "8104.htm", RambamDiagram.Kind.LINTELS),
        RambamDiagram("heichal", "beit_habechirah", 4, 9, "חלוקת ההיכל והאולם", base + "8104.htm", RambamDiagram.Kind.HEICHAL,
            "תרשים עזר לחלוקת החללים המרכזיים בלבד; אינו שחזור מלא של היציעים והמסיבה."),
        RambamDiagram("courtyard", "beit_habechirah", 5, 17, "העזרה ולשכותיה", base + "8105.htm", RambamDiagram.Kind.COURTYARD,
            "תרשים התמצאות סכמטי לפי פרק ה׳; מיקומי לשכות בתוך כל קבוצת לשכות אינם קביעת מידות."),
        RambamDiagram("shoham", "klei_hamikdash", 9, 9, "שמות השבטים באבני השוהם", base + "8209.htm", RambamDiagram.Kind.SHOHAM,
            "סידור השמות לפי ציור הרמב״ם שבנוסח הדפוסים. יהוסף, זבולן ובנימן: כ״ה אותיות בכל אבן."),
        RambamDiagram("ephod", "klei_hamikdash", 9, 9, "צורת האיפוד", base + "8209.htm", RambamDiagram.Kind.EPHOD),
        RambamDiagram("round_beam", "tumat_met", 12, 8, "חלל טפח תחת עמוד עגול", base + "a112.htm", RambamDiagram.Kind.ROUND_BEAM,
            "חתך סכמטי; שיעור היקף כ״ד טפחים לפי הקירובים המתוארים בהלכה, לא חשבון מדויק של π.")
    )

    private val byLocation = all.groupBy { Triple(it.sectionId, it.chapter, it.halacha) }
    fun forHalacha(sectionId: String, chapter: Int, halacha: Int): List<RambamDiagram> =
        byLocation[Triple(sectionId, chapter, halacha)].orEmpty()
}
