package com.example.domain.tanya

data class TanyaSourceLine(val page: Int, val text: String, val section: Int,
    val justified: Boolean = true, val greyFromWord: Int? = null, val greyBeforeWord: Int? = null,
    val largeOpeningWords: Int = 0, val rightIndentFraction: Float = 0f,
    val centered: Boolean = false, val fontScale: Float = 1f,
    val wordFontScales: Map<Int, Float> = emptyMap()) {
    // The verified source represents the next page's catchword as an isolated,
    // non-justified word. Paragraph endings remain centered independently.
    val isCatchword: Boolean get() = !justified && text.trim().none { it.isWhitespace() }
}

/** Line endings checked visually against the clean Otzar770 edition, pages 270–271.
 * This is a verified lesson excerpt, NOT a completed transcription of the whole book.
 * Never fabricate printed line endings for a reference that has not been mapped.
 */
object VerifiedTanyaPrint {
    const val SOURCE = "http://www.otzar770.com/library/main_book.asp?nBookId=108"
    private val references = listOf("23:1-3", "23:4-7", "23:8-9", "23:10-14", "24:1-9")
    private fun ref(part: String) = "Tanya, Part IV; Iggeret HaKodesh $part"
    private val completePages by lazy {
        val previous = listOf(
            "מעשי דבר ומחשבת אדם ותחבולותיו. כי זו מלאכת",
            "שמים היא ולא מלאכת ב״ו. ולהאמין באמונה",
            "שלימה במצות חז״ל הוי שפל רוח בפני כל אדם",
            "בכלל כי יציבא מלתא ותקין פתגמא שכ״א מתוקן",
            "מחבירו. וכתיב כל [איש] ישראל כאיש אחד חברים.",
            "כמו שאיש א׳ מחובר מאברים רבים ובהפרדם נוגע",
            "בלב כי ממנו תוצאות חיים. א״כ אנחנו היות כולנו",
            "כאיש א׳ ממש תיכון העבודה בלב ומכלל הן כו׳.",
            "וע״כ נאמר ולעבדו שכם אחד דוקא. וע״כ אהובי",
            "ידידיי נא ונא לטרוח בכל לב ונפש לתקוע אהבת",
            "רעהו בלבו. ואיש את רעת רעהו אל תחשבו בלבבכם",
            "כתיב ולא תעלה על לב לעולם ואם תעלה יהדפנה",
            "מלבו כהנדוף עשן וכמו מחשבת ע״ז ממש. כי",
            "גדולה לה״ר כנגד ע״ז וג״ע וש״ד. ואם בדבור כך",
            "כו׳ וכבר נודע לכל חכם לב יתרון הכשר המח׳ על",
            "הדבור הן לטוב והן למוטב. וה׳ הטוב המברך את",
            "עמו בשלום ישים עליכם שלום וחיים עד העולם כנפש",
            "או״נ מלונ״ח:"
        ).mapIndexed { i, text -> TanyaSourceLine(270, text, 0, justified = i != 17) }
        val next = listOf(
            "כה להבין אמרי בינה מ״ש בספר הנק׳",
            "צוואת ריב״ש הגם שבאמת",
            "אינה צוואתו ולא ציוה כלל לפני פטירתו רק הם",
            "לקוטי אמרותיו הטהורות שלקטו לקוטי בתר לקוטי",
            "ולא"
        ).mapIndexed { i, text -> TanyaSourceLine(275, text, 0, justified = i != 4) }
        (previous + references.flatMap { forReference(ref(it)) } + next)
            .distinctBy { it.page to it.text }
            .map { line ->
                line.copy(greyFromWord = null, greyBeforeWord = null,
                    largeOpeningWords = if (line.text.startsWith("כג בגזירת") || line.text.startsWith("כד אהוביי") || line.text.startsWith("כה להבין")) 2 else 0,
                    rightIndentFraction = if (line.text == "חכמי המשנה ע״ה ששנו" || line.text == "ליוצרם ושנואים ליצרם ואל יעשה" || line.text == "צוואת ריב״ש הגם שבאמת") .40f else 0f)
            }
    }

    /** Complete source pages, with only this assignment's words in the active colour. */
    fun fullPagesForReference(ref: String): List<TanyaSourceLine> {
        val extended = TanyaPrintExtension.fullPagesForReference(ref)
        if (extended.isNotEmpty()) {
            // Page 275 also contains the end of yesterday's epistle, already verified earlier.
            val known = extended.map { it.page to it.text }.toSet()
            val preceding = if (extended.first().page == 275) completePages.filter {
                it.page == 275 && (it.page to it.text) !in known
            }.map { it.copy(section = 0, greyFromWord = 0) } else emptyList()
            return preceding + extended
        }
        val excerpt = forReference(ref)
        if (excerpt.isEmpty()) return emptyList()
        val active = excerpt.associateBy { it.page to it.text }
        val pages = excerpt.map { it.page }.toSet()
        return completePages.filter { it.page in pages }.map { source ->
            active[source.page to source.text]?.copy(largeOpeningWords = source.largeOpeningWords,
                rightIndentFraction = source.rightIndentFraction) ?: source.copy(section = 0, greyFromWord = 0)
        }
    }

    fun hasDailyWords(line: TanyaSourceLine): Boolean =
        (line.greyFromWord ?: line.text.split(' ').size) > (line.greyBeforeWord ?: 0)

    fun stableId(line: TanyaSourceLine): String = "print_${line.page}_${line.text.hashCode()}"

    /** Retain saved positions from the former excerpt-only layout. */
    fun restoredIndex(ref: String, oldIndex: Int, blockId: String): Int {
        val full = fullPagesForReference(ref)
        if (blockId == "print_intro") return 0
        if (blockId.startsWith("print_")) {
            val index = full.indexOfFirst { stableId(it) == blockId }
            if (index >= 0) return index + 1
        }
        val oldLine = forReference(ref).getOrNull(oldIndex - 1)
            ?: return (full.indexOfFirst { hasDailyWords(it) } + 1).coerceAtLeast(0)
        return (full.indexOfFirst { it.page == oldLine.page && it.text == oldLine.text } + 1).coerceAtLeast(0)
    }

    fun forReference(ref: String): List<TanyaSourceLine> = when (ref) {
        "Tanya, Part IV; Iggeret HaKodesh 23:1-3" -> listOf(
        TanyaSourceLine(270, "כג בגזירת עירין פתגמא ומאמר קדישין", 1),
        TanyaSourceLine(270, "חכמי המשנה ע״ה ששנו", 1),
        TanyaSourceLine(270, "במשנתם עשרה שיושבין ועוסקין בתורה שכינה", 1),
        TanyaSourceLine(270, "שרויה ביניהם כי זה כל האדם ואף גם זאת היתה", 1),
        TanyaSourceLine(270, "כל ירידתו בעוה״ז לצורך עליה זו אשר אין עליה", 1),
        TanyaSourceLine(270, "למעלה הימנה כי שכינת עוזו אשר בגבהי מרומים", 2),
        TanyaSourceLine(270, "והשמים ושמי השמים לא יכלכלו אימתה. תשכון", 2),
        TanyaSourceLine(270, "ותתגדל בתוך בני ישראל כמ״ש כי אני ה׳ שוכן", 2),
        TanyaSourceLine(270, "בתוך בנ״י ע״י עסק התורה והמצות בעשרה דוקא", 2),
        TanyaSourceLine(270, "כמ״ש", 2, justified = false),
        TanyaSourceLine(271, "כמ״ש רז״ל אתיא תוך תוך כו׳ וע״ז נאמר בקרבך", 2),
        TanyaSourceLine(271, "קדוש ואין דבר שבקדושה פחות מעשרה ומשום", 3),
        TanyaSourceLine(271, "הכי נמי אצטריך להו לרז״ל למילף מקרא מנין", 3),
        TanyaSourceLine(271, "שאפילו אחד שיושב ועוסק בתורה כו׳ ואף גם זאת", 3),
        TanyaSourceLine(271, "לא מצאו לו סמך מן המקרא אלא לקביעת שכר", 3),
        TanyaSourceLine(271, "בלבד ליחיד לפי ערכו לפי [נ״א ולפי] ערך המרובים", 3),
        TanyaSourceLine(271, "אבל לענין השראת קדושת הקב״ה אין לו ערך", 3),
        TanyaSourceLine(271, "אליהם כלל וההפרש שבין השראה לקביעת שכר", 3, greyFromWord = 2)
    )
        "Tanya, Part IV; Iggeret HaKodesh 23:4-7" -> listOf(
            TanyaSourceLine(271, "אליהם כלל וההפרש שבין השראה לקביעת שכר", 1, greyBeforeWord = 2),
            TanyaSourceLine(271, "מובן למביני מדע. כי קביעת שכר הוא שמאיר ה׳", 1),
            TanyaSourceLine(271, "לנפש תדרשנו באור תורתו שהוא מעטה לבושו ממש", 1),
            TanyaSourceLine(271, "ולכן נקראת התורה אור שנאמר עוטה אור כשלמה", 1),
            TanyaSourceLine(271, "והנפש היא בעלת גבול ותכלית בכל כחותיה לכן", 1),
            TanyaSourceLine(271, "גם אור ה׳ המאיר בה הוא גבולי מצומצם ומתלבש", 1),
            TanyaSourceLine(271, "בתוכה וע״כ יתפעל לב מבקשי ה׳ בשעת התפלה", 1),
            TanyaSourceLine(271, "וכיוצא בה כי בו ישמח לבם ויגיל אף גילת ורנן", 1),
            TanyaSourceLine(271, "ותתענג נפשם בנועם ה׳ [נ״א על ה׳] ואורו בהגלותו", 1),
            TanyaSourceLine(271, "ממעטה לבושו שהיא התורה ויצא כברק חצו וזו", 1),
            TanyaSourceLine(271, "היא קביעת שכר התורה הקבועה תמיד בנפש עמלה", 1),
            TanyaSourceLine(271, "בה. אבל ההשראה היא הארה עצומה מאור ה׳", 2),
            TanyaSourceLine(271, "המאיר בה בלי גבול ותכלית ואינו יכול להתלבש", 2),
            TanyaSourceLine(271, "בנפש גבולית כ״א מקיף עליה מלמעלה מראשה ועד", 2),
            TanyaSourceLine(271, "רגליה כמו שאמרו חז״ל אכל בי עשרה שכינתא", 2),
            TanyaSourceLine(271, "שריא כלומר עליהם מלמעלה כמ״ש ויהי נועם ה׳", 2),
            TanyaSourceLine(271, "עלינו ומעשה ידינו כוננה עלינו כלומר כי נועם ה׳", 2),
            TanyaSourceLine(271, "אשר הופיע במעשה ידינו בעסק התורה והמצות", 2),
            TanyaSourceLine(271, "דאורייתא וקב״ה כולא חד יתכונן וישרה עלינו", 2),
            TanyaSourceLine(271, "מלמעלה להיותו בלי גבול ותכלית ואינו מתלבש", 2),
            TanyaSourceLine(271, "בנפשנו", 2, justified = false),
            TanyaSourceLine(272, "בנפשנו ושכלנו וע״כ אין אנו משיגים בשכלנו הנעימות", 3),
            TanyaSourceLine(272, "והעריבות מנועם ה׳ וזיו השכינה בלי גבול ותכלית", 3),
            TanyaSourceLine(272, "אשר מתכונן ושורה עלינו במעשה ידינו בתורה", 3),
            TanyaSourceLine(272, "ומצות ברבים דוקא. וע״ז ארז״ל שכר מצוה בהאי", 3),
            TanyaSourceLine(272, "עלמא ליכא כי אי אפשר לעולם להשיגו כי אם", 3),
            TanyaSourceLine(272, "בהתפשטות הנפש מהגוף ואף גם זאת על דרך", 3),
            TanyaSourceLine(272, "החסד כמ״ש ולך ה׳ חסד כי אתה תשלם לאיש", 3),
            TanyaSourceLine(272, "כמעשהו וכמו שארז״ל שהקב״ה נותן כח בצדיקים", 3),
            TanyaSourceLine(272, "כו׳. משא״כ במלאכים כמו ששמעתי מרבותי כי", 4),
            TanyaSourceLine(272, "אילו נמצא מלאך אחד עומד במעמד עשרה מישראל", 4),
            TanyaSourceLine(272, "ביחד אף שאינם מדברים בדברי תורה תפול עליו", 4),
            TanyaSourceLine(272, "אימתה ופחד בלי גבול ותכלית משכינתא דשריא", 4),
            TanyaSourceLine(272, "עליהו עד שהיה מתבטל ממציאותו לגמרי וע״כ רע", 4, greyFromWord = 6)
        )
        "Tanya, Part IV; Iggeret HaKodesh 23:8-9" -> listOf(
            TanyaSourceLine(272, "עליהו עד שהיה מתבטל ממציאותו לגמרי וע״כ רע", 1, greyBeforeWord = 6),
            TanyaSourceLine(272, "בעיני המעשה אשר נעשה תחת השמש בכלל ובפרט", 1),
            TanyaSourceLine(272, "בין אחי ורעי הנגשים אל ה׳ הגשה זו תפלה ואחר", 1),
            TanyaSourceLine(272, "התפלה או לפניה נעשה מושב לצים ר״ל כמו שארז״ל", 1),
            TanyaSourceLine(272, "שנים שיושבין ואין ביניהם ד״ת כו׳ ואם נעשה מושב", 1),
            TanyaSourceLine(272, "לצים בעשרה דשכינתא שריא עלייהו אין לך עלבונא", 1),
            TanyaSourceLine(272, "וקלנא דשכינתא גדול מזה רחמנא ליצלן ואם אמרו", 1),
            TanyaSourceLine(272, "רז״ל על העובר עבירה בסתר שדוחק רגלי השכינה", 1),
            TanyaSourceLine(272, "ח״ו העובר עבירה ברבים דוחק כל שיעור קומה של", 1),
            TanyaSourceLine(272, "יוצר בראשית כביכול כמ״ש רז״ל אין אני והוא וכו׳", 1),
            TanyaSourceLine(272, "אלא שמלך אסור ברהטים כו׳. אבל ווי למאן דדחקין", 2),
            TanyaSourceLine(272, "לשכינתא כד יוקים לה קודשא ב״ה ויימא לה התנערי", 2),
            TanyaSourceLine(272, "מעפר קומי וגו׳ ועל תלת מילין מתעכבי ישראל", 2),
            TanyaSourceLine(272, "בגלותא על דדחקין לשכינתא ועל דעבדין קלנא", 2),
            TanyaSourceLine(272, "בשכינתא וכו׳ כמ״ש בזוה״ק. על כן אהובי אחיי", 2, greyFromWord = 4)
        )
        "Tanya, Part IV; Iggeret HaKodesh 23:10-14" -> listOf(
            TanyaSourceLine(272, "בשכינתא וכו׳ כמ״ש בזוה״ק. על כן אהובי אחיי", 1, greyBeforeWord = 4),
            TanyaSourceLine(272, "ורעי", 1, justified = false),
            TanyaSourceLine(273, "ורעי אל נא תרעו הרעה הגדולה הזאת ותנו כבוד", 1),
            TanyaSourceLine(273, "לה׳ אלהיכם בטרם יחשך דהיינו בין מנחה למעריב", 1),
            TanyaSourceLine(273, "כל ימות החול ללמוד בעשרה פנימיות התורה שהיא", 1),
            TanyaSourceLine(273, "אגדה שבס׳ ע״י שרוב סודות התורה גנוזין בה ומכפרת", 1),
            TanyaSourceLine(273, "עונותיו של אדם כמבואר בכהאריז״ל והנגלות שבה", 1),
            TanyaSourceLine(273, "הן דרכי ה׳ שילך בהם האדם וישית עצות בנפשו", 1),
            TanyaSourceLine(273, "במילי דשמיא ובמילי דעלמא וכידוע לכל חכמי לב", 1),
            TanyaSourceLine(273, "וגם ללמוד מעט בשו״ע או״ח הלכות הצריכות לכל", 2),
            TanyaSourceLine(273, "אדם וע״ז ארז״ל כל השונה הלכות בכל יום כו׳ שהן", 2),
            TanyaSourceLine(273, "הלכות ברורות ופסוקות הלכה למעשה כמבואר", 2),
            TanyaSourceLine(273, "בפרש״י ז״ל שם ובשבת קדש בעלות המנחה יעסקו", 3),
            TanyaSourceLine(273, "בהל׳ שבת כי הלכתא רבתא לשבתא ובקל יכול", 3),
            TanyaSourceLine(273, "האדם ליכשל בה ח״ו אפילו באיסור כרת וסקילה", 3),
            TanyaSourceLine(273, "מחסרון ידיעה ושגגת תלמוד עולה זדון ח״ו ואצ״ל", 3),
            TanyaSourceLine(273, "באיסורי דברי סופרים שרבו כמו רבו למעלה ובפרט", 3),
            TanyaSourceLine(273, "באיסורי מוקצה דשכיחי טובא וחמורים ד״ס יותר", 3),
            TanyaSourceLine(273, "מד״ת כמ״ש רז״ל שכל העובר על דברי חכמים אפילו", 3),
            TanyaSourceLine(273, "באיסור קל של דבריהם כמו האוכל קודם תפלת", 3),
            TanyaSourceLine(273, "ערבית וכה״ג חייב מיתה כעובר על חמורות שבתורה", 3),
            TanyaSourceLine(273, "וכל יחיד אל יפרוש עצמו מן הציבור אפילו ללמוד", 4),
            TanyaSourceLine(273, "ענין אחר כ״א בדבר שהציבור עסוקין בו ואצ״ל שלא", 4),
            TanyaSourceLine(273, "יצא החוצה אם לא יהיו עשרה מבלעדו ועליו אני", 4),
            TanyaSourceLine(273, "קורא הפסוק ועוזבי ה׳ יכלו כו׳ כמשארז״ל על כל", 4),
            TanyaSourceLine(273, "דבר שבקדושה. כי אין קדושה כקדושת התורה", 4),
            TanyaSourceLine(273, "דאורייתא וקב״ה כולא חד. וכל הפורש מן הציבור", 5),
            TanyaSourceLine(273, "כו׳ ושומע לי ישכון בטח ובימיו ובימינו תושע יהודה", 5),
            TanyaSourceLine(273, "וירושלים תשכון לבטח אמן כן יהי רצון:", 5, justified = false)
        )
        "Tanya, Part IV; Iggeret HaKodesh 24:1-9" -> listOf(
            TanyaSourceLine(274, "כד אהוביי אחיי אל נא תרעו ריעים האהובים", 1),
            TanyaSourceLine(274, "ליוצרם ושנואים ליצרם ואל יעשה", 2),
            TanyaSourceLine(274, "אדם עצמו רשע שעה אחת לפני המקום אשר בחר", 2),
            TanyaSourceLine(274, "בה מכל היום להקהל ולעמוד לפניו בשעה זו שהיא", 2),
            TanyaSourceLine(274, "עת רצון לפניו להתגלות לבוא אל המקדש מעט", 2),
            TanyaSourceLine(274, "לפקוד לשכינת כבודו השוכן אתם בתוך טומאותם", 2),
            TanyaSourceLine(274, "ולהמצא לדורשיו ומבקשי ומייחלי והמספר בצרכיו", 3),
            TanyaSourceLine(274, "מראה בעצמו שאינו חפץ להתבונן ולראות בגילוי", 3),
            TanyaSourceLine(274, "כבוד מלכותו ונעשה מרכבה טמאה לכסיל העליון", 3),
            TanyaSourceLine(274, "שנאמר עליו לא יחפץ כסיל בתבונה כו׳ כמ״ש", 3),
            TanyaSourceLine(274, "הזהר והאריז״ל דהיינו שאינו חפץ להתבונן ולראות", 3),
            TanyaSourceLine(274, "ביקר תפארת גדולתו של מלך מה״מ הקב״ה הנגלות", 3),
            TanyaSourceLine(274, "למעלה בשעה זו וגם למטה אל החפצים להביט אל", 3),
            TanyaSourceLine(274, "כבודו וגדלו המתעטף ומתלבש בתוך תיבות התפלה", 3),
            TanyaSourceLine(274, "הסדורה בפי כל ומתגלה לכל אחד לפי שכלו ושורש", 3),
            TanyaSourceLine(274, "נשמתו כדכתיב לפי שכלו יהולל איש יהלל כתיב", 3),
            TanyaSourceLine(274, "ומלכותא דרקיע כעין מלכותא דארעא שדרך המלך", 4),
            TanyaSourceLine(274, "להיות חביון עוזו בחדרי חדרים וכמה שומרים על", 4),
            TanyaSourceLine(274, "הפתחים (עד) אשר כמה וכמה מצפים ימים ושנים", 4),
            TanyaSourceLine(274, "לראות עוזו וכבודו וכשעולה רצונו להתגלות לכל", 4),
            TanyaSourceLine(274, "והעביר קול בכל מלכותו להקהל ולעמוד לפניו", 4),
            TanyaSourceLine(274, "להראותם כבוד מלכותו ויקר תפארת גדולתו מי", 4),
            TanyaSourceLine(274, "שעומד לפניו ואינו חושש לראותו ומתעסק בצרכיו", 4),
            TanyaSourceLine(274, "כמה גרוע וסכל ופתי הוא ונמשל כבהמות נדמה", 4),
            TanyaSourceLine(274, "בעיני כל הבריות וגם הוא בזיון המלך בהראותו", 5),
            TanyaSourceLine(274, "לפניו שאינו ספון בעיניו לקבל נחת ושעשועים מהביט", 5),
            TanyaSourceLine(274, "אל כבודו ויפיו יותר מעסק צרכיו וגם הוא מתחייב", 5),
            TanyaSourceLine(274, "בנפשו", 5, justified = false),
            TanyaSourceLine(275, "בנפשו למלך על הראות קלונו ובזיונו את המלך לעין", 5),
            TanyaSourceLine(275, "כל רואה וע״ז נאמר וכסילים מרים קלון כלומר אף", 5),
            TanyaSourceLine(275, "שהוא כסיל לא יהי׳ מרים קלון שיהי׳ נראה הקלון", 5),
            TanyaSourceLine(275, "לעין כל. וע״כ קבעו חז״ל בתפלה כאלו עומד לפני", 6),
            TanyaSourceLine(275, "המלך עכ״פ יהי׳ מראה בעצמו כאלו עומד כו׳ לעין", 6),
            TanyaSourceLine(275, "כל רואה בעיני בשר אל מעשיו ודיבוריו אף שאין", 6),
            TanyaSourceLine(275, "לו מחשבה לכסיל וע״ז הענין נתקן כל התפלות", 6),
            TanyaSourceLine(275, "למתבונן בהם היטב. ומי שאינו מראה כן מתחייב", 6),
            TanyaSourceLine(275, "בנפשו ועליו אמרו בזוהר הק׳ דאנוהג קלנא בתקונא", 6),
            TanyaSourceLine(275, "עילאה ואחזי פרודא ולית ליה חולקא באלהא דישראל", 6),
            TanyaSourceLine(275, "ר״ל ע״כ שליחותייהו דרז״ל קא עבידנא לגזור גזירה", 7),
            TanyaSourceLine(275, "שוה לכל נפש שלא לשוח שיחה בטלה משיתחיל", 7),
            TanyaSourceLine(275, "הש״ץ להתפלל התפלה עד גמר קדיש בתרא שחרית", 7),
            TanyaSourceLine(275, "ערבית ומנחה וכו׳. והעובר ע״ז בזדון ישב על הארץ", 8),
            TanyaSourceLine(275, "ויבקש מג׳ אנשים שיתירו לו נידוי שלמעלה ושב", 8),
            TanyaSourceLine(275, "ורפא לו ולא חל עליו שום נידוי למפרע כל עיקר", 8),
            TanyaSourceLine(275, "כי מתחלתו לא חל כ״א על המורדים והפושעים שאינם", 8),
            TanyaSourceLine(275, "חוששים כלל לבקש כפרה מן השמים ומן הבריות", 8),
            TanyaSourceLine(275, "על העון פלילי הזה. וגם דוקא כשמדברים בזדון", 9),
            TanyaSourceLine(275, "בשאט נפש ולא על השוכח או שנזרקו מפיו כמה", 9),
            TanyaSourceLine(275, "תיבות בלא מתכוין שא״צ התרה כלל ובוחן לבות", 9),
            TanyaSourceLine(275, "וכליות אלהים צדיק. הטיבה ה׳ לטובים ולישרים בלבותם:", 9, justified = false)
        )
        else -> TanyaPrintExtension.excerpt(ref)
    }
}
