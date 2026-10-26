package com.example.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.R

val BonaNovaFontFamily = FontFamily(
    Font(R.font.bona_nova_regular, FontWeight.Normal),
    Font(R.font.bona_nova_bold, FontWeight.Bold)
)

val LibertinusSerifFontFamily = FontFamily(
    Font(R.font.libertinus_serif_regular, FontWeight.Normal),
    Font(R.font.libertinus_serif_bold, FontWeight.Bold)
)

val FrankRuhlFontFamily = FontFamily(Font(R.font.frank_ruhl_libre))
val TanyaPrintFontFamily = FontFamily(Font(R.font.derech_vilna_regular))

enum class FontStyleOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val fontFamily: FontFamily
) {
    SANS("sans", "נוטו", "נוטו סנס עברית", NotoSansHebrewFontFamily),
    SERIF("serif", "וילנא", "רום וילנא — מותאם לניקוד", TanyaPrintFontFamily),
    LIBERTINUS("libertinus", "ליברטינוס", "ליברטינוס סריף", LibertinusSerifFontFamily),
    BONA_NOVA("bonanova", "בונה נובה", "בונה נובה", BonaNovaFontFamily);

    companion object {
        fun fromId(id: String): FontStyleOption = when (id) {
            "bonanova", "cursive" -> BONA_NOVA
            "libertinus", "monospace" -> LIBERTINUS
            "serif" -> SERIF
            else -> SANS
        }
    }
}
