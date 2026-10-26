package com.example.widget

/**
 * The widget shows Hayom Yom in Hebrew. The bundled entries (Chabadpedia) keep the
 * Rebbe's original, which is partly Yiddish, followed by a Hebrew translation: either
 * inside the paragraph after "----" and "תרגום ללשון הקודש:", or as separate paragraphs
 * after the Yiddish ones. The full original stays in the app's reader.
 */
object HayomYomHebrew {
    private const val SEPARATOR = "----"
    private val TRANSLATION_LABEL = Regex("^\\s*תרגום ללשון הקודש\\s*:\\s*")
    private const val YIDDISH_THRESHOLD = 0.15

    private val yiddishWords = setOf(
        "איז", "דער", "האט", "און", "פון", "ניט", "נישט", "וואס", "זיך", "ער", "זיין", "מיט", "דאס",
        "געווען", "אויף", "אויך", "ווען", "זיי", "דעם", "צו", "א", "אן", "דארף", "זאל", "קען", "וועט",
        "נאר", "ווי", "אזוי", "געזאגט", "האבן", "זענען", "זיינען", "וואו", "ביי", "מען", "איך", "מיר",
        "אים", "זיינע", "ווייל", "איינער", "אבער", "ניטא", "יעדער", "דאך", "עס"
    )
    private val word = Regex("[א-ת'\"״׳]+")

    fun paragraphs(original: List<String>): List<String> {
        val hebrew = original.mapNotNull { paragraph ->
            val separator = paragraph.indexOf(SEPARATOR)
            when {
                separator >= 0 -> paragraph.substring(separator + SEPARATOR.length)
                    .replace(TRANSLATION_LABEL, "").trim().ifEmpty { null }
                yiddishShare(paragraph) >= YIDDISH_THRESHOLD -> null
                else -> paragraph
            }
        }
        return hebrew.ifEmpty { original }
    }

    /** Share of words that are Yiddish function words or Yiddish spellings (וו/יי, -ען). */
    fun yiddishShare(text: String): Double {
        val words = word.findAll(text).map { it.value }.toList()
        if (words.isEmpty()) return 0.0
        val hits = words.count { w ->
            w in yiddishWords || w.startsWith("וו") || w.startsWith("יי") || (w.length > 3 && w.endsWith("ען"))
        }
        return hits.toDouble() / words.size
    }
}
