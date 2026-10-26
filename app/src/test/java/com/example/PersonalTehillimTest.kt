package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.tehillim.PersonalTehillimStore
import com.example.data.tehillim.TehillimRepository
import com.example.domain.tehillim.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PersonalTehillimTest {
    @Test fun chapterChangesOnHebrewBirthdayAndNotRoshHashanah() {
        val entry = PersonalTehillimEntry(birthday = HebrewBirthday(5757, 1, 4))
        val birthday = PersonalPsalmCalendar.toCivil(HebrewBirthday(5787, 1, 4))
        assertEquals(LocalDate.of(2026, 10, 15), birthday)
        assertEquals(30, entry.chapter(birthday.minusDays(1)))
        assertEquals(31, entry.chapter(birthday))
        assertEquals(31, entry.chapter(birthday.plusDays(1)))
        val roshHashanah = PersonalPsalmCalendar.toCivil(HebrewBirthday(5787, 0, 1))
        assertEquals(entry.chapter(roshHashanah.minusDays(1)), entry.chapter(roshHashanah))
    }
    @Test fun newbornAndFirstBirthdayAndFutureBirth() {
        val birth = HebrewBirthday(5787, 1, 4)
        val entry = PersonalTehillimEntry(birthday = birth)
        val day = PersonalPsalmCalendar.toCivil(birth)
        assertNull(entry.chapter(day.minusDays(1)))
        assertEquals(1, entry.chapter(day))
        assertEquals(2, entry.chapter(PersonalPsalmCalendar.toCivil(HebrewBirthday(5788, 1, 4))))
    }
    @Test fun civilConversionHonorsBirthAfterSunset() {
        assertEquals(HebrewBirthday(5787, 1, 4), PersonalPsalmCalendar.fromCivil(LocalDate.of(2026, 10, 15)))
        assertEquals(HebrewBirthday(5787, 1, 5), PersonalPsalmCalendar.fromCivil(LocalDate.of(2026, 10, 15), true))
    }
    @Test fun personalBirthdayTurnsAtSunsetRegardlessOfDailyMidnightPreference() {
        val city = com.example.domain.sunset.SupportedCities.findCityByName("ירושלים")
        val day = LocalDate.of(2026, 10, 15)
        val sunset = com.example.domain.sunset.SolarSunsetCalculator.calculateSunset(day, city, java.time.ZoneId.of(city.defaultZoneId))!!
        assertEquals(day, PersonalPsalmCalendar.readingDate(day, day, city, sunset.minusSeconds(1)))
        assertEquals(day.plusDays(1), PersonalPsalmCalendar.readingDate(day, day, city, sunset))
        assertEquals(day.minusDays(1), PersonalPsalmCalendar.readingDate(day.minusDays(1), day, city, sunset))
    }
    @Test fun adarChoiceChangesOnceAndLeapBirthMonthIsPreserved() {
        val second = PersonalTehillimEntry(birthday = HebrewBirthday(5783, 6, 10))
        val first = second.copy(adar = AdarBirthday.FIRST)
        val adarI = PersonalPsalmCalendar.toCivil(HebrewBirthday(5784, 5, 10))
        val adarII = PersonalPsalmCalendar.toCivil(HebrewBirthday(5784, 6, 10))
        assertEquals(2, first.chapter(adarI)); assertEquals(1, second.chapter(adarI))
        assertEquals(2, first.chapter(adarII)); assertEquals(2, second.chapter(adarII))
        val leapBorn = PersonalTehillimEntry(birthday = HebrewBirthday(5784, 5, 10))
        assertEquals(PersonalPsalmCalendar.toCivil(HebrewBirthday(5785, 6, 10)), PersonalPsalmCalendar.anniversary(leapBorn, 5785))
    }
    @Test fun missingThirtiethUsesTheExplicitUserPolicy() {
        val longYear = (5770..5790).first { runCatching { PersonalPsalmCalendar.toCivil(HebrewBirthday(it, 1, 30)) }.isSuccess }
        val shortYear = (longYear + 1..5800).first { runCatching { PersonalPsalmCalendar.toCivil(HebrewBirthday(it, 1, 30)) }.isFailure }
        val entry = PersonalTehillimEntry(birthday = HebrewBirthday(longYear, 1, 30))
        val next = PersonalPsalmCalendar.toCivil(HebrewBirthday(shortYear, 2, 1))
        assertEquals(next, PersonalPsalmCalendar.anniversary(entry, shortYear))
        assertEquals(next.minusDays(1), PersonalPsalmCalendar.anniversary(entry.copy(missingDay = MissingBirthdayDay.LAST_DAY), shortYear))
    }
    @Test fun duplicateChaptersAndLargeListsRoundTripWithPoliciesAndIds() {
        val entries = (1..250).map { PersonalTehillimEntry(label = "משפחה $it", fixedChapter = 119) } +
            PersonalTehillimEntry(birthday = HebrewBirthday(5783, 6, 1), adar = AdarBirthday.FIRST)
        assertEquals(entries, PersonalTehillimStore.decode(PersonalTehillimStore.encode(entries)))
        assertEquals(251, entries.map { it.id }.toSet().size)
        assertNotEquals(PersonalTehillimStore.completionKey(entries[0].id, "2026-10-15", 119), PersonalTehillimStore.completionKey(entries[1].id, "2026-10-15", 119))
    }
    @Test fun personalPsalm119HasAllVersesWithoutTheMonthlySplit() = runBlocking {
        val repo = TehillimRepository(ApplicationProvider.getApplicationContext<Context>())
        val chapter = repo.getChapter(119)
        assertEquals(176, chapter.verses.size)
        assertEquals((1..176).toList(), chapter.verses.map { it.verseNumber })
        assertTrue(repo.getChapter(11).verses[4].textPlainNikud.startsWith("ה׳"))
    }
    @Test fun noAutomaticWrapAbove150() {
        val entry = PersonalTehillimEntry(birthday = HebrewBirthday(5600, 0, 1))
        assertNull(entry.chapter(LocalDate.of(2026, 10, 15)))
    }
    @Test fun savedEntriesOrderAndCompletionSurviveReopeningTheStore() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = PersonalTehillimStore(context)
        val a = PersonalTehillimEntry(label = "ראשון", fixedChapter = 1)
        val b = PersonalTehillimEntry(label = "שני", fixedChapter = 1)
        val date = LocalDate.of(2026, 10, 15)
        try {
            store.save(a); store.save(b); store.move(b.id, -1); store.complete(a, date, true)
            val restored = PersonalTehillimStore(context).state.first()
            assertTrue(restored.entries.indexOf(b) < restored.entries.indexOf(a))
            assertTrue(PersonalTehillimStore.completionKey(a.id, date.toString(), 1) in restored.completed)
            assertFalse(PersonalTehillimStore.completionKey(b.id, date.toString(), 1) in restored.completed)
            assertFalse(PersonalTehillimStore.completionKey(a.id, date.plusDays(1).toString(), 1) in restored.completed)
            store.delete(a.id)
            val deleted = store.state.first()
            assertFalse(deleted.entries.any { it.id == a.id })
            assertFalse(deleted.completed.any { it.startsWith(a.id) })
        } finally { store.delete(a.id); store.delete(b.id) }
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsAdarIInANonLeapYear() {
        PersonalTehillimEntry(birthday = HebrewBirthday(5783, 5, 10))
    }
}
