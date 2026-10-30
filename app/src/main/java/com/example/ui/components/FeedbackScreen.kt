package com.example.ui.components

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.BuildConfig

private const val FEEDBACK_EMAIL = "frielabsdev@gmail.com"

/** Opens an editable email draft; opening the composer never means feedback was sent. */
@Composable
fun FeedbackScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var screen by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var expected by rememberSaveable { mutableStateOf("") }
    var repeat by rememberSaveable { mutableStateOf("") }
    var noEmail by rememberSaveable { mutableStateOf(false) }
    val details = "גרסה: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n" +
        "מכשיר: ${Build.MANUFACTURER} ${Build.MODEL}\nAndroid: ${Build.VERSION.RELEASE}"
    fun body() = "מסך או שיעור: ${screen.trim()}\n\n" +
        "מה קרה / מה כדאי לשפר:\n${description.trim()}\n\n" +
        "מה ציפיתי שיקרה:\n${expected.trim()}\n\n" +
        "האם זה חוזר ואיך משחזרים:\n${repeat.trim()}\n\n$details"
    androidx.activity.compose.BackHandler(onBack = onDismiss)
    Surface(Modifier.fillMaxSize().testTag("feedback_dialog")) {
        Column(Modifier.fillMaxSize().imePadding().padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("שליחת משוב", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                TextButton(onClick = onDismiss) { Text("חזרה") }
            }
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("תקלה, הצעה או משהו שעבד היטב — נשמח לשמוע. אין צורך לתת דירוג.")
                OutlinedTextField(screen, { screen = it.take(160) }, label = { Text("באיזה מסך או שיעור השתמשת?") },
                    modifier = Modifier.fillMaxWidth().testTag("feedback_screen"))
                OutlinedTextField(description, { description = it.take(4000) }, label = { Text("מה קרה או מה כדאי לשפר?") },
                    minLines = 3, modifier = Modifier.fillMaxWidth().testTag("feedback_description"))
                OutlinedTextField(expected, { expected = it.take(2000) }, label = { Text("מה ציפית שיקרה? (רשות)") },
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(repeat, { repeat = it.take(2000) }, label = { Text("האם זה חוזר? איך משחזרים? (רשות)") },
                    modifier = Modifier.fillMaxWidth())
                Text(details, style = MaterialTheme.typography.bodySmall)
                Text("הפרטים שלמעלה יצורפו לטיוטה אל $FEEDBACK_EMAIL. השליחה תתבצע רק באישור שלך באפליקציית הדוא״ל; אפשר לערוך ולהוסיף צילום מסך שם.",
                    style = MaterialTheme.typography.bodySmall)
                if (noEmail) Text("לא נמצאה אפליקציית דוא״ל. אפשר להעתיק את המשוב ולשלוח ל־$FEEDBACK_EMAIL.")
                TextButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("משוב בדרך", "אל: $FEEDBACK_EMAIL\n\n${body()}"))
                    Toast.makeText(context, "המשוב הועתק", Toast.LENGTH_SHORT).show()
                }, enabled = description.isNotBlank(), modifier = Modifier.testTag("feedback_copy")) {
                    Text("העתקת המשוב")
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(enabled = description.isNotBlank(), modifier = Modifier.testTag("feedback_email"), onClick = {
                val subject = Uri.encode("משוב בדרך · ${BuildConfig.VERSION_CODE}")
                val uri = Uri.parse("mailto:$FEEDBACK_EMAIL?subject=$subject&body=${Uri.encode(body())}")
                try {
                    context.startActivity(Intent(Intent.ACTION_SENDTO, uri))
                    noEmail = false
                } catch (_: ActivityNotFoundException) {
                    noEmail = true
                }
            }) { Text("פתיחה בדוא״ל") }
        }
    }
}
