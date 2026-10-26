package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.speech.*
import com.example.data.tanya.TanyaPlaybackState

@Composable
fun DailySpeechControls(state: DailySpeechState, dateLabel: String, track: String,
    onStart: () -> Unit, onAction: (String) -> Unit, onSeek: (Long) -> Unit = {},
    source: String = "michael", onSourceChange: ((String) -> Unit)? = null) {
    val dark=MaterialTheme.colorScheme.background.luminance()<.3f
    val paper=if(dark) Color(0xFF1E222A) else Color.White
    val ink=if(dark) Color(0xFFF4F5F5) else Color(0xFF171714)
    val accent=Color(0xFFFFB38C)
    var restart by remember {mutableStateOf(false)}
    Card(modifier=Modifier.fillMaxWidth().padding(vertical=12.dp).testTag("daily_speech_controls")
        .border(1.dp,ink.copy(alpha=.15f),RoundedCornerShape(20.dp)),shape=RoundedCornerShape(20.dp),
        colors=CardDefaults.cardColors(containerColor=paper,contentColor=ink)) {
        Column(Modifier.padding(12.dp)) {
            Text("האזנה לרמב״ם היומי",fontWeight=FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(if(state.active) state.chapter else "$dateLabel · ${if(track=="three") "שלושה פרקים ברצף" else "פרק אחד"}",fontSize=13.sp)
            if (onSourceChange != null) Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf("recording" to if(track=="three") "ר׳ אליהו פרץ" else "הרב נדב כהן", "michael" to "הקראה · Michael").forEach { (key,label) ->
                    FilterChip(selected=source==key, onClick={onSourceChange(key)},
                        modifier=Modifier.weight(1f).testTag("rambam_source_$key"),
                        label={Text(label,fontSize=12.sp)},
                        colors=FilterChipDefaults.filterChipColors(labelColor=ink,
                            selectedContainerColor=accent.copy(alpha=.24f),selectedLabelColor=ink))
                }
            }
            Text(if(state.active) state.voiceStatus else if(source=="recording") "שיעור מוקלט · מקור: חב״ד FM" else state.voiceStatus,
                fontSize=12.sp,color=ink.copy(alpha=.65f))
            if(!state.active && source=="michael") Text("הקול יורד פעם אחת (כ־64MB), ולאחר מכן ההקראה ללא אינטרנט.",fontSize=12.sp,color=ink.copy(alpha=.65f))
            // Exactly the same player as Tanya: no second transport design or system voice settings.
            TanyaPlaybackControls(TanyaPlaybackState(active=state.active,playing=state.playing,
                loading=state.preparing && !state.paused,position=state.position,duration=state.duration,speed=state.speed,error=state.error),
                onPlay={if(!state.active) { if(state.preparing) onAction(DailySpeechStatus.STOP) else onStart() } else onAction(DailySpeechStatus.TOGGLE)},
                onSeek=onSeek,onSpeed={onAction(DailySpeechStatus.SPEED)},ink=ink,paper=paper,accent=accent)
            if(state.active) {
                if (!state.recorded) {
                // Same direction as the transport above: previous on the left, next on the right.
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                        TextButton(onClick={onAction(DailySpeechStatus.PREVIOUS)},enabled=state.canPrevious,
                            modifier=Modifier.testTag("daily_speech_previous")) {
                            Icon(Icons.Default.SkipPrevious,null,Modifier.size(18.dp));Text("הלכה קודמת",fontSize=12.sp)
                        }
                        TextButton(onClick={onAction(DailySpeechStatus.NEXT)},enabled=state.canNext,
                            modifier=Modifier.testTag("daily_speech_next")) {
                            Text("הלכה הבאה",fontSize=12.sp);Icon(Icons.Default.SkipNext,null,Modifier.size(18.dp))
                        }
                    }
                }
                }
                TextButton(onClick={restart=true},modifier=Modifier.testTag("daily_speech_restart")) {Text("מהתחלה",fontSize=12.sp)}
            }
            if(source=="recording") Text("הקלטה מלאה של השיעור; הדילוג לפי הלכה זמין בהקראת Michael.",fontSize=11.sp,color=ink.copy(alpha=.65f))
            state.error?.let {Text(it,color=MaterialTheme.colorScheme.error,fontSize=13.sp,modifier=Modifier.testTag("daily_speech_error"))}
        }
    }
    if(restart) AlertDialog(onDismissRequest={restart=false},title={Text("להתחיל את השיעור מחדש?")},
        text={Text("ההקראה תחזור לתחילת הרמב״ם היומי.")},
        confirmButton={TextButton(onClick={restart=false;onAction("daily_speech.restart")}){Text("מהתחלה")}},
        dismissButton={TextButton(onClick={restart=false}){Text("ביטול")}})
}
