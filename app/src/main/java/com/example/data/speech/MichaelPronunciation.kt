package com.example.data.speech

import android.content.Context
import com.example.domain.speech.DailySpeechText
import org.json.JSONObject
import java.text.Normalizer

/** Prepared from the bundled corpus; the reader's original text is never altered. */
class MichaelPronunciation(context: Context) {
    private val words: JSONObject = context.assets.open("speech/michael-pronunciation.json")
        .bufferedReader().use { JSONObject(it.readText()).getJSONObject("words") }

    /** Upcoming lessons only, prepared in sentence context by tools/build_michael_context.py. */
    private val sentences: JSONObject = runCatching {
        context.assets.open("speech/michael-context.json").bufferedReader().use { JSONObject(it.readText()).getJSONObject("sentences") }
    }.getOrElse { JSONObject() }

    fun phonemes(text: String): String {
        val out=mutableListOf<String>()
        val sentence=mutableListOf<String>()
        fun flush() {
            val words=sentence.filter { it.first() in 'א'..'ת' }
            val prepared=sentences.optJSONArray(sentenceKey(words))?.takeIf { it.length()==words.size }
            var index=0
            sentence.forEach { token ->
                if(token.first() !in 'א'..'ת') out+=token
                else (divineName(token) ?: prepared?.getString(index) ?: word(token)).let { index++; if(it.isNotBlank()) out+=it }
            }
            sentence.clear()
        }
        TOKEN.findAll(text).forEach { match ->
            sentence+=match.value
            if(match.value.first() in ".:;?!") flush()
        }
        flush()
        return out.joinToString(" ").replace(Regex(" ([.,;:!?])"),"$1")
            .also { check(it.isNotBlank()) { "אין טקסט מתאים להקראה." } }
    }

    private fun word(token:String):String {
        val key=canonical(token)
        check(words.has(key)) { "טרם הוכנה הגייה לקטע הזה. נסו לעדכן את האפליקציה." }
        return words.getString(key)
    }

    companion object {
        private val TOKEN=Regex("[א-ת][א-ת\\u0591-\\u05C7׳״'\"]*|[.,;:!?]")
        private val OMIT=Regex("[׳״'\"\\u0591-\\u05AF\\u05BD\\u05BF\\u05C0\\u05C4\\u05C5]")
        internal fun canonical(text:String)=Normalizer.normalize(text,Normalizer.Form.NFD).replace(OMIT,"")
        internal fun sentenceKey(words:List<String>)=DailySpeechText.hash(words.joinToString(" ") { canonical(it) }).take(16)
        private val MARKS=Regex("[\\u0591-\\u05C7]")
        // An unvowelled ה with geresh, or יְיָ, after at most two prefix letters. לָהּ׳, משֶׁה׳ and שֶׂה׳ do not match.
        private val DIVINE=Regex("((?:[ובלמכש][\\u05B0-\\u05C7]*){0,2}?)(?:ה['׳\"״]+|י[\\u05B0-\\u05C7]*י[\\u05B0-\\u05C7]*['׳\"״]*)")
        private val DIVINE_PREFIX=mapOf("" to "haʃˈem","ו" to "vehaʃˈem","ל" to "laʃˈem","ב" to "baʃˈem","מ" to "mehaʃˈem",
            "כ" to "kaʃˈem","ש" to "ʃehaʃˈem","וב" to "ubaʃˈem","ול" to "velaʃˈem","ומ" to "umehaʃˈem")
        /** ה׳ and יְיָ are read as "השם"; the prepared index would otherwise drop them or spell out the letters. */
        internal fun divineName(token:String):String? {
            val match=DIVINE.matchEntire(Normalizer.normalize(token,Normalizer.Form.NFD)) ?: return null
            val prefix=match.groupValues[1]
            if('ׂ' in prefix) return null
            return DIVINE_PREFIX[MARKS.replace(prefix,"")]
        }
    }
}
