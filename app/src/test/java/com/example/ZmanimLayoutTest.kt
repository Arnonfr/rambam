package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.*
import com.example.data.local.UserPreferences
import com.example.domain.zmanim.*
import com.example.ui.DateDrawerState
import com.example.ui.components.*
import com.example.ui.screens.DateDrawer
import com.example.ui.theme.*
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "iw-rIL-w400dp-h900dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ZmanimLayoutTest {
    val compose = createComposeRule()
    @get:Rule val hostAndCompose = withComposeHost(compose)
    private val date = LocalDate.parse("2026-10-21")
    private val location = ZmanimLocation("ירושלים",31.7683,35.2137,"Asia/Jerusalem")
    @Test fun `compact header fits at 320 360 and 390 with correct RTL order and unchanged budget`() {
        var width by mutableStateOf(320)
        var enabled by mutableStateOf(true)
        val prefs = ZmanimPreferences(fixedLocation = location)
        val day = ZmanimCalculator().calculate(date,location)
        var selected: ZmanId? = null
        compose.setContent { DailyRambamTheme {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column(Modifier.width(width.dp).background(Color.White)) {
                    DateDrawer(date,date,DateDrawerState.COLLAPSED,null,{ "י׳ חשוון תשפ״ז" },{},{},{},
                        times = { ZmanimHeader(prefs.copy(enabled = enabled),day,Instant.parse("2026-10-21T09:00:00Z")) { selected = it } })
                    Text("השיעורים שלי", Modifier.testTag("first_content"))
                }
            }
        } }
        listOf(320,360,390).forEach { size ->
            compose.runOnIdle { width = size }
            ZmanId.defaults.forEach { compose.onNodeWithTag("zmanim_${it.name}").assertIsDisplayed().assertHeightIsAtLeast(44.dp) }
            val sunrise=compose.onNodeWithTag("zmanim_SUNRISE").fetchSemanticsNode().boundsInRoot
            val noon=compose.onNodeWithTag("zmanim_NOON").fetchSemanticsNode().boundsInRoot
            val sunset=compose.onNodeWithTag("zmanim_SUNSET").fetchSemanticsNode().boundsInRoot
            assertTrue(sunrise.left >= noon.right - 1 && noon.left >= sunset.right - 1)
            val density=compose.density.density
            val content=compose.onNodeWithTag("first_content").fetchSemanticsNode().boundsInRoot
            assertTrue("Header must fit prior 106dp budget: ${content.top/density}",content.top/density<=106)
            compose.onRoot().captureRoboImage("build/review/zmanim-header-$size.png")
        }
        compose.onNodeWithTag("zmanim_SUNRISE").performClick()
        assertEquals(ZmanId.SUNRISE,selected)
        val top=compose.onNodeWithTag("first_content").fetchSemanticsNode().boundsInRoot.top
        compose.runOnIdle { enabled=false }
        compose.onNodeWithTag("zmanim_header").assertDoesNotExist()
        assertTrue(compose.onNodeWithTag("first_content").fetchSemanticsNode().boundsInRoot.top < top)
    }
    @Test fun `location search has no list until typing and stays in normal preferences`() {
        var prefs by mutableStateOf(ZmanimPreferences())
        compose.setContent { DailyRambamTheme {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                com.example.ui.screens.SettingsScreen(
                    preferences=UserPreferences(),onBack={},onDayBoundaryChange={},onCityChange={},onFontFamilyChange={},
                    onToggleStudy={},onTanyaBookViewChange={},onOpenAttribution={},
                    zmanimPreferences=prefs,onZmanimChange={prefs=it},expandZmanim=true)
            }
        } }
        compose.onNodeWithTag("settings_screen").assertExists()
        compose.onNodeWithTag("zmanim_sheet").assertDoesNotExist()
        compose.onNodeWithTag("zmanim_city_search").assertDoesNotExist()
        compose.onNodeWithTag("zmanim_city_ירושלים").assertDoesNotExist()
        compose.onNodeWithTag("zmanim_enabled").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("zmanim_change_city").performScrollTo().performClick()
        compose.onNodeWithTag("zmanim_city_search").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("zmanim_city_ירושלים").assertDoesNotExist()
        compose.onNodeWithTag("zmanim_city_search").performTextInput("ירו")
        compose.onNodeWithTag("zmanim_city_ירושלים").performScrollTo().performClick()
        assertEquals(location,prefs.location)
        compose.onNodeWithTag("zmanim_city_search_panel").assertDoesNotExist()
        compose.onNodeWithTag("settings_screen").assertExists()
        compose.onNodeWithTag("zmanim_enabled").performScrollTo().performClick()
        assertFalse(prefs.enabled)
        compose.onNodeWithTag("settings_civil_date").performScrollTo().performClick()
        assertTrue(prefs.showCivilDate)
        compose.onNodeWithTag("settings_screen").captureRoboImage("build/review/zmanim-regular-settings-135.png")
    }
    @Test fun `labels hours and original icons share one RTL line and civil date is optional`() {
        var civil by mutableStateOf(false)
        var prefs by mutableStateOf(ZmanimPreferences())
        val day=ZmanimCalculator().calculate(date,prefs.location!!)
        compose.setContent { DailyRambamTheme {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column(Modifier.width(320.dp).background(Color.White)) {
                    DateDrawer(date,date,DateDrawerState.COLLAPSED,null,{"י׳ חשוון תשפ״ז"},{},{},{},showCivilDate=civil,
                        times={ZmanimHeader(prefs,day,Instant.parse("2026-10-21T09:00:00Z")) {}})
                }
            }
        } }
        compose.onNodeWithTag("home_civil_date").assertDoesNotExist()
        compose.onRoot().captureRoboImage("build/review/zmanim-inline-320-135.png")
        ZmanId.defaults.forEach { id ->
            val label=compose.onNodeWithTag("zmanim_label_${id.name}",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
            val time=compose.onNodeWithTag("zmanim_time_${id.name}",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
            val icon=compose.onNodeWithTag("zmanim_icon_${id.name}",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
            assertEquals(label.center.y,time.center.y,2f)
            assertTrue(icon.left>=label.right-1)
            assertTrue(label.left>=time.right-1)
            listOf("zmanim_label_${id.name}","zmanim_time_${id.name}").forEach { tag ->
                val layouts=mutableListOf<TextLayoutResult>()
                compose.onNodeWithTag(tag,useUnmergedTree=true).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult){it(layouts)}
                assertFalse("$tag: ${layouts.single().size}, ${layouts.single().layoutInput.constraints}, lines=${layouts.single().lineCount}, width=${layouts.single().didOverflowWidth}, height=${layouts.single().didOverflowHeight}",layouts.single().hasVisualOverflow)
            }
        }
        compose.onRoot().captureRoboImage("build/review/zmanim-inline-320-135.png")
        compose.runOnIdle {civil=true}
        compose.onRoot().captureRoboImage("build/review/zmanim-inline-civil-135.png")
        compose.onNodeWithTag("home_civil_date",useUnmergedTree=true).assertIsDisplayed()
        val hebrew=compose.onNodeWithText("רביעי י׳ חשוון",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
        val civilBounds=compose.onNodeWithTag("home_civil_date",useUnmergedTree=true).fetchSemanticsNode().boundsInRoot
        assertEquals(hebrew.center.y,civilBounds.center.y,2f)
        compose.onRoot().captureRoboImage("build/review/zmanim-inline-civil-135.png")
        compose.runOnIdle {prefs=prefs.copy(enabled=false)}
        compose.onNodeWithTag("zmanim_header").assertDoesNotExist()
    }
    @Test fun `many chosen times can scroll and the introduction is limited to two visits`() {
        var visit by mutableStateOf(0)
        var hints by mutableStateOf(0)
        val prefs=ZmanimPreferences(selected=ZmanId.entries.filter{it!=ZmanId.CANDLES})
        val day=ZmanimCalculator().calculate(date,prefs.location!!)
        compose.setContent { DailyRambamTheme {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                key(visit) {
                    Box(Modifier.width(320.dp)) {
                        ZmanimHeader(prefs.copy(scrollHintsShown=hints),day,Instant.parse("2026-10-21T09:00:00Z"),onScrollHintShown={hints++}) {}
                    }
                }
            }
        } }
        compose.waitUntil(5000){hints==1}
        compose.waitForIdle()
        compose.runOnIdle {visit++}
        compose.waitUntil(5000){hints==2}
        compose.waitForIdle()
        compose.runOnIdle {visit++}
        compose.mainClock.advanceTimeBy(2000)
        compose.runOnIdle {assertEquals(2,hints)}
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(10)
        compose.onNodeWithTag("zmanim_NIGHT").assertIsDisplayed()
    }
    @Test fun `Friday entry appears first with a candle icon and all original times remain available`() {
        val friday=date.plusDays(2)
        val prefs=ZmanimPreferences(scrollHintsShown=2)
        val day=ZmanimCalculator().calculate(friday,prefs.location!!)
        compose.setContent { DailyRambamTheme {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column(Modifier.width(360.dp).background(Color.White)) {
                    DateDrawer(friday,friday,DateDrawerState.COLLAPSED,null,{"י״ב חשוון תשפ״ז"},{},{},{},
                        times={ZmanimHeader(prefs,day,Instant.parse("2026-10-23T09:00:00Z")){}})
                    Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        ZmanId.entries.forEach { ZmanimIcon(it,Modifier.size(18.dp)) }
                    }
                }
            }
        } }
        compose.onNodeWithTag("zmanim_CANDLES").assertIsDisplayed()
        compose.onNodeWithTag("zmanim_label_CANDLES",useUnmergedTree=true).assertTextEquals("כניסת שבת")
        compose.onNodeWithTag("zmanim_icon_CANDLES",useUnmergedTree=true).assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/review/zmanim-friday-icons-135.png")
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(3)
        compose.onNodeWithTag("zmanim_SUNSET").assertIsDisplayed()
    }
    @Test fun `loading a published Friday table restores candles to the start of the strip`() {
        val friday=LocalDate.parse("2026-10-23")
        val prefs=ZmanimPreferences(method=ZmanimMethod.SINGAVI,scrollHintsShown=2)
        val loaded=com.example.data.zmanim.SingaviPublishedDay.parse(
            javaClass.classLoader!!.getResourceAsStream("singavi-telaviv-2026-10-23.json")!!.bufferedReader().use { it.readText() },
            friday,prefs.location!!,expectedCity="תל אביב")
        var day by mutableStateOf(loaded.copy(candleLighting=null))
        compose.setContent { DailyRambamTheme {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Box(Modifier.width(360.dp)) { ZmanimHeader(prefs,day,Instant.parse("2026-10-23T09:00:00Z")){} }
            }
        } }
        compose.onNodeWithTag("zmanim_SUNRISE").assertIsDisplayed()
        compose.runOnIdle { day=loaded }
        compose.onNodeWithTag("zmanim_CANDLES").assertIsDisplayed()
        compose.onNodeWithTag("zmanim_time_CANDLES",useUnmergedTree=true).assertTextEquals("17:45")
        compose.onRoot().captureRoboImage("build/review/zmanim-singavi-140.png")
    }
    @Test fun `reader preserves original Tanya punctuation and uses the selected font`() {
        val source="הרבי היה אומר על אצילות “למעלה”. רמב״ם · ה׳ · \"אצי\""
        FontStyleOption.entries.forEach { font ->
            val result=withStableHebrewMarks(AnnotatedString(source),font.fontFamily)
            assertEquals(source,result.text)
            assertTrue(result.spanStyles.isEmpty())
        }
        compose.setContent { DailyRambamTheme {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column(Modifier.fillMaxWidth().background(Color.White).padding(16.dp)) {
                    FontStyleOption.entries.forEach { font ->
                        Text(font.title)
                        StudyTextBlock("",source,source,UserPreferences(fontFamily=font.id,fontSizeSp=26f),textColor=Color.Black)
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        } }
        compose.onRoot().captureRoboImage("build/review/reader-quotes-all-fonts.png")
    }
}
