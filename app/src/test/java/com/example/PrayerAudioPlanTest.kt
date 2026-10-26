package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.prayers.*
import com.example.data.prayers.audio.*
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PrayerAudioPlanTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private fun clip(id: String, approved: Boolean = true) = PrayerAudioClip(id, "https://example.org/$id.m4a", approved)
    @Test fun `missing recording stops contiguous playback without silently skipping`() {
        val steps = listOf(PrayerAudioStep("a", "א", "a", clip = clip("a")),
            PrayerAudioStep("b", "ב", "b"), PrayerAudioStep("c", "ג", "c", clip = clip("c")))
        val run = PrayerAudioPlan.run(steps, "a", DayOfWeek.MONDAY, false)
        assertEquals(listOf("a"), run.steps.map { it.id })
        assertEquals("b", run.blockedAt?.id)
        assertEquals(listOf("c"), PrayerAudioPlan.run(steps, "c", DayOfWeek.MONDAY, false).steps.map { it.id })
    }
    @Test fun `drafts are never playable in production`() {
        assertFalse(PrayerAudioPlan.playable(clip("draft", false), DayOfWeek.MONDAY, false))
        assertTrue(PrayerAudioPlan.playable(clip("draft", false), DayOfWeek.MONDAY, true))
        assertFalse(PrayerAudioPlan.playable(PrayerAudioClip("local", "asset:///prayer-audio/a.m4a", true), DayOfWeek.MONDAY, false))
        assertFalse(PrayerAudioPlan.playable(PrayerAudioClip("unsafe", "http://example.org/a", true), DayOfWeek.MONDAY, true))
    }
    @Test fun `weekday recordings cannot bleed across days`() {
        val c = clip("monday").copy(weekday = DayOfWeek.MONDAY)
        assertTrue(PrayerAudioPlan.playable(c, DayOfWeek.MONDAY, false))
        assertFalse(PrayerAudioPlan.playable(c, DayOfWeek.TUESDAY, true))
    }
    @Test fun `empty or unknown start never starts playback`() {
        assertTrue(PrayerAudioPlan.run(emptyList(), "x", DayOfWeek.MONDAY, true).steps.isEmpty())
        assertTrue(PrayerAudioPlan.run(listOf(PrayerAudioStep("a", "א", "a", clip = clip("a"))), "x", DayOfWeek.MONDAY, true).steps.isEmpty())
    }
    @Test fun `catalog selects exactly one matching shir shel yom and release catalog has no drafts`() {
        DayOfWeek.entries.filter { it != DayOfWeek.SATURDAY }.forEach { day ->
            val selected = PrayerAudioCatalog.load(context, day, true)["shir_shel_yom"]!!
            assertEquals(day, selected.weekday)
            assertFalse(selected.approved)
            context.assets.open(selected.uri.removePrefix("asset:///")).use { assertTrue(it.available() > 0) }
        }
        assertNull(PrayerAudioCatalog.load(context, DayOfWeek.SATURDAY, true)["shir_shel_yom"])
        assertTrue(PrayerAudioCatalog.load(context, DayOfWeek.MONDAY, false).isEmpty())
    }
    private fun shacharit(date: LocalDate) =
        PrayerServices.group(PrayerRepository(context).load(date).sections).single { it.id == "shacharit" }
    private val order get() = PrayerAudioOrder.load(context)
    private fun steps(date: LocalDate, minyan: Boolean = false): List<PrayerAudioStep> {
        val day = PrayerDayRules.of(date)
        return PrayerAudioPlan.forService(shacharit(date), PrayerAudioCatalog.load(context, date.dayOfWeek, true),
            order, day.flags(minyan))
    }

    @Test fun `every order anchor resolves to the displayed text and uses known conditions`() {
        val known = PrayerDayRules.of(LocalDate.of(2026, 9, 28)).flags(true) + PrayerDayRules.of(LocalDate.of(2026, 10, 19)).flags(true) +
            setOf("avinuMalkeinu", "tenDaysOfRepentance", "fast", "purim", "chanukah", "roshChodesh")
        assertEquals(emptyList<String>(), PrayerAudioPlan.unresolvedAnchors(shacharit(LocalDate.of(2026, 10, 19)), order))
        order.flatMap { it.conditions }.map { it.removePrefix("!") }.forEach { assertTrue(it, it in known) }
    }

    @Test fun `every debug clip belongs to a step of the order`() {
        val ids = order.map { it.id }.toSet()
        DayOfWeek.entries.filter { it != DayOfWeek.SATURDAY }.forEach { day ->
            PrayerAudioCatalog.load(context, day, true).keys.forEach { assertTrue(it, it in ids) }
        }
    }

    @Test fun `day without tachanun plays the closing through to the end without lamnatzeach`() {
        val date = LocalDate.of(2026, 10, 5) // Monday, 24 Tishrei
        val plan = steps(date)
        listOf("vidui", "vehu_rachum", "tachanun_end", "closing_lamnatzeach", "closing_tefilah_ledavid", "closing_kaddish")
            .forEach { id -> assertTrue(id, plan.none { it.id == id }) }
        val run = PrayerAudioPlan.run(plan, "closing_ashrei", date.dayOfWeek, true)
        assertEquals(listOf("closing_ashrei", "closing_uvaletzion", "closing_beit_yaakov", "closing_psalm124", "shir_shel_yom",
            "hoshienu", "ein_kelokeinu", "ketoret_end", "tana", "aleinu", "final_verses"), run.steps.map { it.id })
        assertNull(run.blockedAt)
        assertEquals("shir-mon-v2", run.steps.single { it.id == "shir_shel_yom" }.clip?.id)
    }

    @Test fun `monday with tachanun stops at the missing shema and offers the next recorded section`() {
        val date = LocalDate.of(2026, 10, 19)
        val plan = steps(date)
        val opening = PrayerAudioPlan.run(plan, "hareni", date.dayOfWeek, true)
        assertEquals("hareni", opening.steps.first().id)
        assertEquals("yishtabach", opening.steps.last().id)
        assertEquals("shema_blessings", opening.blockedAt?.id)
        assertEquals("vehu_rachum", PrayerAudioPlan.nextPlayable(plan, "shema_blessings", date.dayOfWeek, true)?.id)
        // Daily vidui and nefilat apayim are not recorded: starting tachanun stops at once.
        assertEquals("vidui", PrayerAudioPlan.run(plan, "vidui", date.dayOfWeek, true).blockedAt?.id)
        val end = PrayerAudioPlan.run(plan, "vehu_rachum", date.dayOfWeek, true)
        assertEquals(listOf("vehu_rachum", "tachanun_end", "closing_ashrei", "closing_lamnatzeach", "closing_uvaletzion",
            "closing_tefilah_ledavid", "closing_beit_yaakov", "closing_psalm124", "shir_shel_yom", "hoshienu",
            "ein_kelokeinu", "ketoret_end", "tana", "aleinu", "final_verses"), end.steps.map { it.id })
    }

    @Test fun `rosh chodesh keeps musaf and barchi nafshi as stops instead of skipping them`() {
        val date = LocalDate.of(2026, 11, 11)
        val plan = steps(date)
        assertTrue(plan.none { it.id == "closing_lamnatzeach" })
        val run = PrayerAudioPlan.run(plan, "closing_ashrei", date.dayOfWeek, true)
        assertEquals("musaf", run.blockedAt?.id)
        assertEquals("barchi_nafshi", PrayerAudioPlan.run(plan, "shir_shel_yom", date.dayOfWeek, true).blockedAt?.id)
    }

    @Test fun `minyan sections exist only in minyan mode and are missing there`() {
        val date = LocalDate.of(2026, 10, 19)
        assertTrue(steps(date).none { it.id == "closing_kaddish" || it.id == "torah_reading" })
        val minyan = steps(date, minyan = true)
        assertEquals("closing_kaddish", PrayerAudioPlan.run(minyan, "closing_uvaletzion", date.dayOfWeek, true).blockedAt?.id)
        assertTrue(minyan.any { it.id == "torah_reading" })
    }
}
