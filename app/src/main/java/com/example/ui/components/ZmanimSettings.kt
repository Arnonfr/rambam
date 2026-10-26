package com.example.ui.components

import android.location.Geocoder
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.sunset.SupportedCities
import com.example.domain.zmanim.*
import com.example.data.zmanim.OfflineTimezone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/** Lives in the existing preferences scroll view; only a focused location search opens a dialog. */
@Composable
fun ZmanimSettingsSection(preferences: ZmanimPreferences, locating: Boolean = false, message: String? = null,
    initiallyExpanded: Boolean = false, onUpdate: (ZmanimPreferences) -> Unit, onLocate: () -> Unit = {}, standalone: Boolean = false) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    var chooseCity by rememberSaveable { mutableStateOf(false) }
    var reset by remember { mutableStateOf(false) }
    val uri = LocalUriHandler.current
    Column(Modifier.fillMaxWidth().testTag("zmanim_preferences_section")) {
        ZmanimToggle("הצגת זמני היום",preferences.enabled,"zmanim_enabled") { onUpdate(preferences.copy(enabled=it)) }
        if (!standalone) TextButton(onClick={expanded=!expanded},modifier=Modifier.fillMaxWidth().testTag("settings_zmanim"),
            contentPadding=PaddingValues(0.dp)) {
            Column(Modifier.weight(1f)) {
                Text("התאמת זמני היום",Modifier.fillMaxWidth(),fontWeight=FontWeight.Medium)
                Text(preferences.location?.name ?: "מיקום לא זמין",Modifier.fillMaxWidth(),fontSize=12.sp,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(if(expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,null)
        }
        if(expanded || standalone) {
            Text("שיטת הזמנים", fontWeight=FontWeight.Medium, modifier=Modifier.padding(top=12.dp))
            ZmanimMethod.entries.forEach { method ->
                Row(Modifier.fillMaxWidth().clickable { onUpdate(preferences.copy(method=method)) }
                    .testTag("zmanim_method_${method.name}"), verticalAlignment=Alignment.CenterVertically) {
                    RadioButton(preferences.method == method, onClick={onUpdate(preferences.copy(method=method))})
                    Text(method.label, fontSize=13.sp)
                }
            }
            if(preferences.method == ZmanimMethod.SINGAVI) Text("זמני הלוח המקורי מהאתר, לפי עיר קבועה. נדרש אינטרנט לעדכון היומי; אין חישוב למיקום GPS או לתאריך שאינו זמין בלוח. ציצית ותפילין וסוף זמן תפילה אינם מסופקים בלוח.",fontSize=12.sp)

            Text("מיקום",fontWeight=FontWeight.Medium,modifier=Modifier.padding(top=12.dp))
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(preferences.location?.name ?: "מיקום לא זמין",fontSize=14.sp)
                    if(preferences.mode=="gps") Text("לפי המיקום שלי",fontSize=12.sp)
                }
                TextButton(onClick={chooseCity=true},modifier=Modifier.testTag("zmanim_change_city")) { Text("שינוי") }
            }
            TextButton(onClick=onLocate,enabled=!locating,modifier=Modifier.testTag("zmanim_gps")) {
                Icon(Icons.Default.MyLocation,null,Modifier.size(16.dp));Spacer(Modifier.width(6.dp))
                Text(if(locating) "מאתר מיקום…" else "לפי המיקום שלי")
            }
            if(preferences.mode=="gps" && preferences.fixedLocation!=null) TextButton(onClick={onUpdate(preferences.copy(mode="fixed"))}) {
                Text("חזרה ל${preferences.fixedLocation.name}")
            }
            message?.let { Text(it,color=MaterialTheme.colorScheme.error,fontSize=12.sp) }
            if(chooseCity) ZmanimCitySearch(onDismiss={chooseCity=false},onSelect={ city ->
                onUpdate(preferences.copy(mode="fixed",fixedLocation=city));chooseCity=false
            })
            HorizontalDivider(Modifier.padding(vertical=12.dp))
            ZmanimToggle("הדגשת הזמן הבא",preferences.highlightNext,"zmanim_highlight") { onUpdate(preferences.copy(highlightNext=it)) }
            ZmanimToggle("כניסת שבת וחג כשיש",preferences.showCandleLighting,"zmanim_candles") { onUpdate(preferences.copy(showCandleLighting=it)) }
            if(preferences.showCandleLighting) {
                val minutes=preferences.candleLightingMinutes ?: if(preferences.method == ZmanimMethod.SINGAVI) 18 else CandleLightingRules.defaultMinutes(preferences.location ?: ZmanimPreferences.defaultLocation)
                Row(Modifier.fillMaxWidth().heightIn(min=48.dp),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("דקות לפני השקיעה",fontSize=13.sp)
                        Text(if(preferences.candleLightingMinutes==null) "לפי מנהג המיקום" else "מותאם אישית",fontSize=11.sp)
                    }
                    IconButton(onClick={onUpdate(preferences.copy(candleLightingMinutes=(minutes-1).coerceAtLeast(5)))},enabled=minutes>5) { Icon(Icons.Default.Remove,"פחות דקה") }
                    Text(minutes.toString(),fontSize=14.sp,modifier=Modifier.testTag("zmanim_candle_minutes"))
                    IconButton(onClick={onUpdate(preferences.copy(candleLightingMinutes=(minutes+1).coerceAtMost(60)))},enabled=minutes<60) { Icon(Icons.Default.Add,"עוד דקה") }
                }
                if(preferences.candleLightingMinutes!=null) TextButton(onClick={onUpdate(preferences.copy(candleLightingMinutes=null))}) { Text("חזרה למנהג המיקום") }
                Text("בחג שמתחיל במוצאי שבת או חג, מוצג זמן הדלקה אחרי צאת היום הקדוש, מאש קיימת.",fontSize=11.sp)
                Text("לוח החגים",fontSize=13.sp,modifier=Modifier.padding(top=8.dp))
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                    listOf(null to "לפי המיקום",true to "ישראל",false to "חו״ל").forEach { (value,label) ->
                        FilterChip(selected=preferences.holidayInIsrael==value,onClick={onUpdate(preferences.copy(holidayInIsrael=value))},label={Text(label,fontSize=11.sp)})
                    }
                }
            }
            HorizontalDivider(Modifier.padding(vertical=12.dp))
            Text("הזמנים שלי",fontWeight=FontWeight.Medium)
            Text("בחרו זמנים. החצים משנים את הסדר שלהם בשורה.",fontSize=11.sp,modifier=Modifier.padding(bottom=8.dp))
            val options=preferences.selected + ZmanId.entries.filter { it!=ZmanId.CANDLES && it !in preferences.selected }
            options.forEach { id ->
                val checked=id in preferences.selected
                val index=preferences.selected.indexOf(id)
                fun move(delta:Int) {
                    val list=preferences.selected.toMutableList()
                    if(index>=0 && index+delta in list.indices) {
                        java.util.Collections.swap(list,index,index+delta);onUpdate(preferences.copy(selected=list))
                    }
                }
                Row(Modifier.fillMaxWidth().heightIn(min=48.dp),verticalAlignment=Alignment.CenterVertically) {
                    Row(Modifier.weight(1f).heightIn(min=48.dp).toggleable(checked,role=Role.Checkbox,
                        enabled=!checked || preferences.selected.size>1,onValueChange={ value ->
                            onUpdate(preferences.copy(selected=if(value) preferences.selected+id else preferences.selected-id))
                        }).testTag("zmanim_select_${id.name}"),verticalAlignment=Alignment.CenterVertically) {
                        Checkbox(checked,null,enabled=!checked || preferences.selected.size>1)
                        ZmanimIcon(id,Modifier.size(18.dp));Spacer(Modifier.width(8.dp))
                        Text(id.labelFor(preferences.method),fontSize=13.sp)
                    }
                    if(checked) {
                        IconButton(onClick={move(-1)},enabled=index>0,modifier=Modifier.size(44.dp)) { Icon(Icons.Default.KeyboardArrowUp,"הקדמת ${id.label}") }
                        IconButton(onClick={move(1)},enabled=index<preferences.selected.lastIndex,modifier=Modifier.size(44.dp)) { Icon(Icons.Default.KeyboardArrowDown,"איחור ${id.label}") }
                    }
                }
            }
            TextButton(onClick={reset=true}) { Text("איפוס תצוגת הזמנים") }
            HorizontalDivider(Modifier.padding(vertical=12.dp))
            Text(preferences.method.label,fontSize=12.sp)
            TextButton(onClick={uri.openUri(preferences.method.source)}) { Text("מקור ושיטת החישוב") }
        }
    }
    if(reset) AlertDialog(onDismissRequest={reset=false},title={Text("איפוס תצוגת הזמנים")},
        text={Text("להחזיר את זריחה, חצות ושקיעה ואת תצוגת כניסת השבת והחג? המיקום יישמר.")},
        confirmButton={TextButton(onClick={onUpdate(preferences.copy(selected=ZmanId.defaults,showCandleLighting=true,highlightNext=true));reset=false}) { Text("איפוס") }},
        dismissButton={TextButton(onClick={reset=false}) { Text("ביטול") }})
}

@Composable
private fun ZmanimCitySearch(onDismiss:()->Unit,onSelect:(ZmanimLocation)->Unit) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var remote by remember { mutableStateOf<List<ZmanimLocation>>(emptyList()) }
    var message by remember { mutableStateOf<String?>(null) }
    var searching by remember { mutableStateOf(false) }
    val cities=remember {
        SupportedCities.CITIES.map { ZmanimLocation(it.nameHebrew,it.latitude,it.longitude,it.defaultZoneId) } +
            ZmanimLocation("פרדס חנה־כרכור",32.474,34.977,"Asia/Jerusalem")
    }
    val matches=if(query.trim().length<2) emptyList() else cities.filter { city ->
        city.name.contains(query.trim(),true) || SupportedCities.CITIES.any { it.nameHebrew==city.name && it.nameEnglish.contains(query.trim(),true) }
    }
        Column(Modifier.fillMaxWidth().padding(vertical=8.dp).testTag("zmanim_city_search_panel")) {
            OutlinedTextField(query,onValueChange={query=it;remote=emptyList();message=null},singleLine=true,
                label={Text("חיפוש יישוב")},modifier=Modifier.fillMaxWidth().testTag("zmanim_city_search"))
            if(query.trim().length<2) Text("הקלידו שם יישוב לחיפוש",fontSize=12.sp,modifier=Modifier.padding(top=12.dp))
            (matches+remote).distinctBy { Triple(it.name,it.latitude,it.longitude) }.forEach { city ->
                TextButton(onClick={onSelect(city)},modifier=Modifier.fillMaxWidth().testTag("zmanim_city_${city.name}")) { Text(city.name,Modifier.fillMaxWidth()) }
            }
            if(query.trim().length>=2) TextButton(enabled=!searching,onClick={
                val search=query.trim();searching=true
                scope.launch {
                    val results=withContext(Dispatchers.IO) {
                        @Suppress("DEPRECATION")
                        runCatching { Geocoder(context,Locale.forLanguageTag("he")).getFromLocationName(search,8).orEmpty().map { address ->
                            val name=listOfNotNull(address.locality ?: address.subAdminArea ?: address.featureName,address.countryName).distinct().joinToString(", ")
                            ZmanimLocation(name.ifBlank {search},address.latitude,address.longitude,OfflineTimezone.at(context,address.latitude,address.longitude))
                        } }.getOrDefault(emptyList())
                    }
                    if(query.trim()==search) { remote=results;message=if(results.isEmpty()) "לא נמצאו תוצאות או שהחיפוש אינו זמין כרגע." else null }
                    searching=false
                }
            }) {Text(if(searching) "מחפש…" else "חיפוש יישובים נוספים")}
            message?.let {Text(it,fontSize=12.sp)}
            TextButton(onClick=onDismiss) {Text("סיום חיפוש")}
        }
}

@Composable
fun ZmanimToggle(label:String,value:Boolean,tag:String,change:(Boolean)->Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min=52.dp).toggleable(value,role=Role.Switch,onValueChange=change)
        .testTag(tag),verticalAlignment=Alignment.CenterVertically) {
        Text(label,Modifier.weight(1f),fontSize=14.sp)
        Switch(value,onCheckedChange=null)
    }
}
