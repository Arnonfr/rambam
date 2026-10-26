package com.example.data.reviews

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import com.example.data.prayers.audio.AudioPlaybackActivity
import com.example.data.speech.DailySpeechStatus
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory

class PlayReviewCoordinator(
    private val activity:ComponentActivity,
    private val engagement:ReviewEngagement=ReviewEngagement(activity),
    private val manager:ReviewManager=ReviewManagerFactory.create(activity),
    private val playInstall:()->Boolean={isPlayInstall(activity)},
    private val audioBusy:()->Boolean={audioBusy(activity)}
) {
    private var requesting=false

    fun requestAfterStudy(safeScreen:()->Boolean) {
        fun safe()=!activity.isFinishing && !activity.isDestroyed &&
            activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && safeScreen() && !audioBusy()
        if(requesting || !playInstall() || !safe() || !engagement.eligible()) return
        if(!engagement.claimAttempt()) return
        requesting=true
        try {
            manager.requestReviewFlow().addOnCompleteListener { result ->
                if(!result.isSuccessful || !safe()) {requesting=false;return@addOnCompleteListener}
                try {
                    manager.launchReviewFlow(activity,result.result).addOnCompleteListener {requesting=false}
                } catch (_:Exception) {requesting=false}
            }
        } catch (_:Exception) {requesting=false}
        // Errors and quota suppression must never interrupt the user's normal flow.
    }

    fun openStore():Boolean {
        val market=Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id=$STORE_PACKAGE"))
            .setPackage("com.android.vending")
        val web=Intent(Intent.ACTION_VIEW,Uri.parse("https://play.google.com/store/apps/details?id=$STORE_PACKAGE"))
        return try {
            activity.startActivity(market);engagement.manualStoreVisit();true
        } catch (_:Exception) {
            try {activity.startActivity(web);engagement.manualStoreVisit();true}
            catch (_:Exception) {false}
        }
    }

    companion object {
        const val STORE_PACKAGE="com.aistudio.dailyrambam.kzjvw"
        @Suppress("DEPRECATION")
        private fun isPlayInstall(context:Context):Boolean {
            if(context.packageName!=STORE_PACKAGE) return false
            return runCatching {
                val installer=if(Build.VERSION.SDK_INT>=30) context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
                    else context.packageManager.getInstallerPackageName(context.packageName)
                installer=="com.android.vending"
            }.getOrDefault(false)
        }
        private fun audioBusy(context:Context):Boolean {
            val speech=DailySpeechStatus.state.value
            return AudioPlaybackActivity.busy || speech.preparing || (speech.active && !speech.completed) ||
                (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager).isMusicActive
        }
    }
}
