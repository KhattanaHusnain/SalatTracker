package com.salattracker.feature.goals

import com.salattracker.domain.model.GoalCategory
import com.salattracker.domain.model.GoalFrequency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class GoalFormStateTest {

    @Test
    fun `numeric fields only accept digits`() {
        val form = GoalFormState()
        assertEquals("12", form.withTargetAmount("12").targetAmount)
        assertEquals("1", form.withTargetAmount("1a").targetAmount) // rejected, unchanged
        assertEquals("", form.withTargetAmount("").targetAmount)
        assertEquals("30", form.withDurationDays("x").durationDays)
    }

    @Test
    fun `blank or zero numbers fall back to safe defaults`() {
        val form = GoalFormState(targetAmount = "", durationDays = "0")
        assertEquals(1, form.amount)
        assertEquals(30, form.duration)
    }

    @Test
    fun `weekdays are stored only for daily goals and all seven means every day`() {
        val picked = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)
        assertEquals(picked, GoalFormState(daysOfWeek = picked).effectiveDays)
        assertTrue(GoalFormState(frequency = GoalFrequency.WEEKLY, daysOfWeek = picked).effectiveDays.isEmpty())
        assertTrue(GoalFormState(daysOfWeek = DayOfWeek.values().toSet()).effectiveDays.isEmpty())
    }

    @Test
    fun `toggling a weekday adds then removes it`() {
        val on = GoalFormState().withDayToggled(DayOfWeek.FRIDAY)
        assertEquals(setOf(DayOfWeek.FRIDAY), on.daysOfWeek)
        assertTrue(on.withDayToggled(DayOfWeek.FRIDAY).daysOfWeek.isEmpty())
    }

    @Test
    fun `a template pre-fills the fields but leaves reminder and end settings alone`() {
        val base = GoalFormState(reminderEnabled = false, endsAfterDuration = true, durationDays = "7")
        val form = base.withTemplate(GoalTemplate.FAST_MON_THU, "Fast Mon & Thu")

        assertEquals("Fast Mon & Thu", form.name)
        assertEquals(GoalCategory.FASTING, form.category)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), form.daysOfWeek)
        assertEquals(false, form.reminderEnabled)
        assertEquals("7", form.durationDays)
    }

    @Test
    fun `Tasbih template explicitly enables its counter`() {
        val form = GoalFormState().withTemplate(GoalTemplate.TASBIH, "Tasbih")

        assertEquals(GoalCategory.DHIKR, form.category)
        assertEquals("Time(s)", form.unit)
        assertTrue(form.usesTasbihCounter)
    }
}
