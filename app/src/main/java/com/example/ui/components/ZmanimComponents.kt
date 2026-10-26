package com.example.ui.components

import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.*
import com.example.domain.zmanim.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.ui.text.rememberTextMeasurer
import kotlinx.coroutines.flow.first
import java.time.*
import java.time.format.DateTimeFormatter

@Composable
fun ZmanimHeader(preferences: ZmanimPreferences, day: ZmanimDay?, now: Instant,
    onScrollHintShown: () -> Unit = {}, onOpen: (ZmanId?) -> Unit) {
    if (!preferences.enabled) return
    val location = preferences.location
    val entries = day?.headerTimes(preferences) ?: preferences.selected.map { ZmanResult(it,null) to it.shortLabel }
    if (entries.isEmpty()) return
    val state = key(preferences.method, day?.date, day?.candleLighting != null) { rememberLazyListState() }
    val context = LocalContext.current
    val latestHint by rememberUpdatedState(onScrollHintShown)
    val density = LocalDensity.current
    val dragged by state.interactionSource.collectIsDraggedAsState()
    var hintedThisVisit by remember { mutableStateOf(false) }
    val motionAllowed = remember(context) {
        android.provider.Settings.Global.getFloat(context.contentResolver,android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,1f) > 0f &&
            !(context.getSystemService(android.content.Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager).isTouchExplorationEnabled
    }
    LaunchedEffect(entries.map { it.first.id },dragged,motionAllowed) {
        if (dragged || hintedThisVisit || preferences.scrollHintsShown >= 2 || !motionAllowed) return@LaunchedEffect
        snapshotFlow { state.layoutInfo.visibleItemsInfo.isNotEmpty() }.first { it }
        kotlinx.coroutines.delay(700)
        if (!state.canScrollForward || state.isScrollInProgress || state.firstVisibleItemIndex != 0 || state.firstVisibleItemScrollOffset != 0) return@LaunchedEffect
        hintedThisVisit = true
        latestHint()
        val moved = state.animateScrollBy(with(density) { 36.dp.toPx() },tween(500))
        kotlinx.coroutines.delay(120)
        state.animateScrollBy(-moved,tween(500))
    }
    val next = if (preferences.highlightNext && day != null && day.date == now.atZone(day.zone).toLocalDate())
        entries.map { it.first }.filter { it.isUpcoming(now) }.minByOrNull { it.rawInstant!! }?.id else null
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.bodySmall.copy(fontSize=11.sp)
    val timeStyle = MaterialTheme.typography.bodyMedium.copy(fontSize=13.sp,fontWeight=FontWeight.Medium,
        textDirection=TextDirection.Ltr,fontFeatureSettings="tnum")
    BoxWithConstraints(Modifier.fillMaxWidth().background(Color.White).testTag("zmanim_header")) {
        val baseWidth = maxWidth / minOf(3,entries.size)
        LazyRow(state=state,flingBehavior=rememberSnapFlingBehavior(state),modifier=Modifier.fillMaxWidth()
            .semantics { if(state.canScrollForward || state.canScrollBackward) stateDescription="ניתן לגלול לזמנים נוספים" }) {
            items(entries,key={it.first.id.name}) { (result,label) ->
                val time = if(day==null) "…" else result.display(day.zone)
                val labelWidth = with(density) { measurer.measure(label,labelStyle).size.width.toDp() } + 1.dp
                val timeWidth = with(density) { measurer.measure(time,timeStyle).size.width.toDp() } + 1.dp
                val textWidth = labelWidth + timeWidth
                val cellWidth = maxOf(baseWidth,textWidth + 40.dp)
                val color = if(result.id==next) Color(0xFF655198) else Color(0xFF4B4A45)
                Row(Modifier.width(cellWidth).heightIn(min=44.dp).clickable { onOpen(result.id) }
                    .semantics(mergeDescendants=true) { contentDescription="$label, $time, ${location?.name.orEmpty()}, ${location?.status(now).orEmpty()}" }
                    .testTag("zmanim_${result.id.name}").padding(horizontal=6.dp),
                    verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
                    ZmanimIcon(result.id,Modifier.size(14.dp).testTag("zmanim_icon_${result.id.name}"),color)
                    Spacer(Modifier.width(4.dp))
                    Text(label,style=labelStyle,color=color,maxLines=1,modifier=Modifier.width(labelWidth).testTag("zmanim_label_${result.id.name}"))
                    Spacer(Modifier.width(5.dp))
                    Text(time,style=timeStyle,color=color,maxLines=1,modifier=Modifier.width(timeWidth).testTag("zmanim_time_${result.id.name}"))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ZmanimSheet(date: LocalDate, fullDate: String, preferences: ZmanimPreferences, day: ZmanimDay?,
    now: Instant, focus: ZmanId?, onOpenSettings: () -> Unit, onDismiss: () -> Unit) {
    var expanded by remember { mutableStateOf(focus) }
    val uri = LocalUriHandler.current
    val focusedRequester = remember { BringIntoViewRequester() }
    LaunchedEffect(day, focus) {
        if (focus != null) { withFrameNanos { }; focusedRequester.bringIntoView() }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(Modifier.fillMaxWidth().fillMaxHeight(.92f).testTag("zmanim_sheet")) {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        Text("זמני היום",Modifier.weight(1f),fontSize=23.sp,fontWeight=FontWeight.Bold)
                        IconButton(onClick=onOpenSettings,modifier=Modifier.testTag("zmanim_open_preferences")) {
                            Icon(Icons.Default.Tune,"העדפות זמני היום")
                        }
                    }
                    Text(fullDate + if(preferences.showCivilDate) " · ${date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}" else "", fontSize = 13.sp)
                    preferences.location?.let { location ->
                        Text("${location.name} · ${location.status(now)}", fontSize = 13.sp)
                        Text("השעות לפי ${location.timezoneId}", fontSize = 12.sp)
                    }
                    Text(preferences.method.label, fontSize = 12.sp, color = Color(0xFF655198))
                }
                HorizontalDivider(Modifier.padding(top = 12.dp))
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {

                        Text(day?.sourceMessage ?: if(preferences.method == ZmanimMethod.SINGAVI) "הזמנים מתוך לוח הרב סינגאווי. אין להמתין לרגע האחרון." else "הזמנים מחושבים. אין להמתין לרגע האחרון.", fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 12.dp))
                        (day?.let { it.times + listOfNotNull(it.candleLighting?.result) }
                            ?: ZmanId.entries.filter { it != ZmanId.CANDLES }.map { ZmanResult(it, null) })
                            .sortedWith(compareBy<ZmanResult> { it.rawInstant == null }.thenBy { it.rawInstant }).forEach { result ->
                            val selected = result.id in preferences.selected || (result.id==ZmanId.CANDLES && preferences.showCandleLighting)
                            val label = if(result.id==ZmanId.CANDLES) day?.candleLighting?.label ?: result.id.label else result.id.labelFor(preferences.method)
                            Column(Modifier.fillMaxWidth().background(if (expanded == result.id) Color(0xFFF4F0FA) else Color.White)
                                .clickable { expanded = if (expanded == result.id) null else result.id }.padding(vertical = 12.dp, horizontal = 8.dp)
                                .testTag("zmanim_detail_${result.id.name}")
                                .then(if (result.id == focus) Modifier.bringIntoViewRequester(focusedRequester) else Modifier)) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(label, fontSize = 16.sp)
                                        if (selected) Text("מוצג בכותרת", fontSize = 11.sp, color = Color(0xFF655198))
                                    }
                                    Text(if (day == null) "…" else result.display(day.zone), fontSize = 20.sp,
                                        style = TextStyle(textDirection = TextDirection.Ltr, fontFeatureSettings = "tnum"))
                                }
                                day?.let { result.dayOffset(date, it.zone) }?.takeIf { it != 0L }?.let { offset ->
                                    Text(if (offset < 0) "יום קודם" else "יום הבא", fontSize = 12.sp)
                                }
                                if (expanded == result.id) {
                                    Text(if(result.id==ZmanId.CANDLES) day?.candleLighting?.explanation.orEmpty() else result.id.explanationFor(preferences.method), fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                                    if (result.rawInstant == null && day != null) Text("אינו ניתן לחישוב במיקום ובתאריך אלה לפי השיטה. לא הופעלה שיטת תחליף.", fontSize = 12.sp)
                                    Text("עיגול לתצוגה: " + when { result.id == ZmanId.NOON -> "לדקה הקרובה, לנקודת אמצע היום"; !result.roundUp -> "כלפי מטה, למועד אחרון"; else -> "כלפי מעלה, בלי להקדים את הזמן" }, fontSize = 11.sp)
                                    TextButton(onClick = { uri.openUri(preferences.method.source) }) { Text("מקור שיטת החישוב") }
                                }
                            }
                            HorizontalDivider()
                        }
                        TextButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth().testTag("zmanim_customize")) { Text("התאמה אישית") }
                    Spacer(Modifier.height(30.dp))
                }
            }
        }
    }
}
