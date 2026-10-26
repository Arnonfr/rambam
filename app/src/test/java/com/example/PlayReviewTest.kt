package com.example

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ReadingStateManager
import com.example.data.reviews.PlayReviewCoordinator
import com.example.data.reviews.ReviewEngagement
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.testing.FakeReviewManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class PlayReviewTest {
    private val app get()=ApplicationProvider.getApplicationContext<Context>()
    private var clock=Instant.parse("2026-10-23T12:00:00Z").toEpochMilli()
    private val day=86400000L
    @Before fun reset() {
        app.getSharedPreferences("play_review_engagement",Context.MODE_PRIVATE).edit().clear().commit()
        app.getSharedPreferences("rambam_reading_anchor",Context.MODE_PRIVATE).edit().clear().commit()
    }
    private fun engaged():ReviewEngagement {
        val store=ReviewEngagement(app){clock}
        repeat(3) {store.recordUse();clock+=day}
        repeat(5) {store.completed("study-$it","2026-10-23")}
        return store
    }

    @Test fun realStudyCompletionsAreDeduplicatedAndNeedThreeActualUsageDays() {
        val store=ReviewEngagement(app){clock}
        val reader=ReadingStateManager(app)
        repeat(10) {store.recordUse();reader.setCompleted("tanya","2026-10-23");reader.setCompleted("tanya","2026-10-23",false)}
        repeat(4) {reader.setCompleted("study-$it","2026-10-23")}
        assertFalse(store.eligible())
        repeat(2) {clock+=day;store.recordUse()}
        assertTrue(store.eligible())
    }

    @Test fun requestAttemptsHaveNinetyDayCooldownAndNeedNewCompletions() {
        val store=engaged()
        assertTrue(store.claimAttempt());assertFalse(store.claimAttempt())
        repeat(5) {store.completed("new-$it","2026-10-24")}
        clock+=89*day;assertFalse(store.eligible())
        clock+=day;assertTrue(store.claimAttempt())
        clock+=90*day;assertFalse(store.eligible())
    }

    @Test fun firebaseBuildsAndAudioNeverConsumeOrStartAutomaticReviewRequests() {
        val activity=Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val store=engaged();val manager=ControlledManager(activity)
        PlayReviewCoordinator(activity,store,manager,{false},{false}).requestAfterStudy {true}
        PlayReviewCoordinator(activity,store,manager,{true},{true}).requestAfterStudy {true}
        assertEquals(0,manager.requests);assertTrue(store.eligible())
    }

    @Test fun delayedReviewResponseCannotOpenOverAudioOrAnotherScreen() {
        val activity=Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val store=engaged();val manager=ControlledManager(activity)
        var safe=true;var audio=false
        val coordinator=PlayReviewCoordinator(activity,store,manager,{true},{audio})
        coordinator.requestAfterStudy {safe};coordinator.requestAfterStudy {safe}
        assertEquals(1,manager.requests)
        audio=true;safe=false;manager.resolve()
        assertEquals(0,manager.launches)
    }

    @Test fun nativeReviewLaunchesWithoutPreRatingQuestionsAndNoResultIsTreatedAsAReview() {
        val activity=Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val store=engaged();val manager=ControlledManager(activity)
        PlayReviewCoordinator(activity,store,manager,{true},{false}).requestAfterStudy {true}
        manager.resolve()
        assertEquals(1,manager.launches)
        assertFalse(store.eligible()) // An attempt, not a claim that the user actually rated.
    }

    @Test fun requestFailureLeavesNormalAppFlowUntouched() {
        val activity=Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val manager=ControlledManager(activity)
        PlayReviewCoordinator(activity,engaged(),manager,{true},{false}).requestAfterStudy {true}
        manager.pending.setException(IllegalStateException("Play unavailable"));shadowOf(android.os.Looper.getMainLooper()).idle()
        assertEquals(0,manager.launches);assertFalse(activity.isFinishing)
        assertNull(shadowOf(activity).nextStartedActivity)
    }

    @Test fun leavingTheAppBeforeTheResponsePreventsNativeDialog() {
        val host=Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val manager=ControlledManager(host.get())
        PlayReviewCoordinator(host.get(),engaged(),manager,{true},{false}).requestAfterStudy {true}
        host.pause().stop();manager.resolve()
        assertEquals(0,manager.launches)
    }

    @Test fun fixedStoreButtonFallsBackToWebAndAlwaysTargetsTheRealStorePackage() {
        val activity=Robolectric.buildActivity(NoPlayActivity::class.java).setup().get()
        val store=engaged()
        val coordinator=PlayReviewCoordinator(activity,store,ControlledManager(activity),{false},{false})
        assertTrue(coordinator.openStore())
        val intent=shadowOf(activity).nextStartedActivity
        assertEquals("https://play.google.com/store/apps/details?id=com.aistudio.dailyrambam.kzjvw",intent.dataString)
        assertFalse(store.eligible())
    }

    class NoPlayActivity:ComponentActivity() {
        override fun startActivity(intent:Intent) {
            if(intent.`package`=="com.android.vending") throw ActivityNotFoundException()
            super.startActivity(intent)
        }
    }

    private class ControlledManager(activity:ComponentActivity):ReviewManager {
        private val fake=FakeReviewManager(activity)
        val pending=TaskCompletionSource<ReviewInfo>()
        var requests=0;var launches=0
        override fun requestReviewFlow():Task<ReviewInfo> {requests++;return pending.task}
        override fun launchReviewFlow(activity:android.app.Activity,info:ReviewInfo):Task<Void> {
            launches++;return Tasks.forResult(null)
        }
        fun resolve() {pending.setResult(fake.requestReviewFlow().result);shadowOf(android.os.Looper.getMainLooper()).idle()}
    }
}
