package com.salattracker.feature.goals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.core.ui.shortLabel
import com.salattracker.core.ui.formatAppTime
import com.salattracker.core.ui.salatSwitchColors
import com.salattracker.domain.model.GoalCategory
import com.salattracker.domain.model.GoalFrequency
import java.time.DayOfWeek
import java.time.LocalTime

// ── Shared form model + fields for Add Goal and Edit Goal ────────────────────

val GOAL_UNITS = listOf("Time(s)", "Minutes", "Pages")

/** One-tap starting points for the Add form — they only pre-fill fields, nothing is saved until Create. */
enum class GoalTemplate(
    val nameRes: Int,
    val category: GoalCategory,
    val frequency: GoalFrequency,
    val target: Int,
    val unit: String,
    val days: Set<DayOfWeek> = emptySet(),
) {
    READ_QURAN(R.string.template_read_quran, GoalCategory.QURAN, GoalFrequency.DAILY, 5, "Pages"),
    MORNING_ADHKAR(R.string.template_morning_adhkar, GoalCategory.DHIKR, GoalFrequency.DAILY, 1, "Time(s)"),
    EVENING_ADHKAR(R.string.template_evening_adhkar, GoalCategory.DHIKR, GoalFrequency.DAILY, 1, "Time(s)"),
    FAST_MON_THU(R.string.template_fast_mon_thu, GoalCategory.FASTING, GoalFrequency.DAILY, 1, "Time(s)", setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)),
    AL_KAHF_FRIDAY(R.string.template_al_kahf, GoalCategory.QURAN, GoalFrequency.DAILY, 1, "Time(s)", setOf(DayOfWeek.FRIDAY)),
    TAHAJJUD(R.string.template_tahajjud, GoalCategory.SALAT, GoalFrequency.DAILY, 1, "Time(s)"),
    WEEKLY_SADAQAH(R.string.template_sadaqah, GoalCategory.CHARITY, GoalFrequency.WEEKLY, 1, "Time(s)"),
    TASBIH(R.string.template_tasbih, GoalCategory.DHIKR, GoalFrequency.DAILY, 1, "Time(s)"),
}

data class GoalFormState(
    val name: String = "",
    val category: GoalCategory = GoalCategory.SALAT,
    val frequency: GoalFrequency = GoalFrequency.DAILY,
    val targetAmount: String = "1",
    val unit: String = GOAL_UNITS.first(),
    val reminderEnabled: Boolean = true,
    val reminderTime: LocalTime = LocalTime.of(4, 30),
    /** Due weekdays for DAILY goals; empty = every day. */
    val daysOfWeek: Set<DayOfWeek> = emptySet(),
    val endsAfterDuration: Boolean = false,
    val durationDays: String = "30",
    val usesTasbihCounter: Boolean = false,
) {
    val amount: Int get() = targetAmount.toIntOrNull()?.takeIf { it > 0 } ?: 1
    val duration: Int get() = durationDays.toIntOrNull()?.takeIf { it > 0 } ?: 30

    /** What gets stored: weekdays only apply to daily goals, and selecting all 7 is just "every day". */
    val effectiveDays: Set<DayOfWeek> get() =
        if (frequency == GoalFrequency.DAILY && daysOfWeek.size < 7) daysOfWeek else emptySet()

    fun withTargetAmount(value: String) = if (value.isEmpty() || value.all(Char::isDigit)) copy(targetAmount = value) else this
    fun withDurationDays(value: String) = if (value.isEmpty() || value.all(Char::isDigit)) copy(durationDays = value) else this
    fun withDayToggled(day: DayOfWeek) = copy(daysOfWeek = if (day in daysOfWeek) daysOfWeek - day else daysOfWeek + day)
    fun withTemplate(template: GoalTemplate, localizedName: String) = copy(
        name = localizedName, category = template.category, frequency = template.frequency,
        targetAmount = template.target.toString(), unit = template.unit, daysOfWeek = template.days,
        usesTasbihCounter = template == GoalTemplate.TASBIH,
    )
}

/** Everything the shared form can do; both Add and Edit wire their ViewModel into this. */
data class GoalFormActions(
    val onNameChanged: (String) -> Unit,
    val onCategoryChanged: (GoalCategory) -> Unit,
    val onFrequencyChanged: (GoalFrequency) -> Unit,
    val onTargetAmountChanged: (String) -> Unit,
    val onUnitChanged: (String) -> Unit,
    val onDayToggled: (DayOfWeek) -> Unit,
    val onEveryDaySelected: () -> Unit,
    val onReminderEnabledChanged: (Boolean) -> Unit,
    val onReminderTimeChanged: (LocalTime) -> Unit,
    val onEndsAfterChanged: (Boolean) -> Unit,
    val onDurationDaysChanged: (String) -> Unit,
    val onUsesTasbihCounterChanged: (Boolean) -> Unit,
    val onTemplateSelected: ((GoalTemplate, String) -> Unit)? = null,
)

private val SUNDAY_FIRST = listOf(
    DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
)

/** The form body shared by Add and Edit. Templates show only when [GoalFormActions.onTemplateSelected] is set. */
@Composable
fun GoalFormFields(form: GoalFormState, actions: GoalFormActions, use24HourFormat: Boolean) {
    var showTimePicker by remember { mutableStateOf(false) }

    actions.onTemplateSelected?.let { onTemplate ->
        FormCard {
            Text(stringResource(R.string.addgoal_templates), color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GoalTemplate.values().forEach { template ->
                    val templateName = stringResource(template.nameRes)
                    Surface(
                        onClick = { onTemplate(template, templateName) },
                        shape = RoundedCornerShape(50),
                        color = SalatSurfaceContainerLow,
                        border = BorderStroke(1.dp, SalatOutlineVariant),
                    ) {
                        Text(
                            templateName, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            color = SalatOnSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily, maxLines = 1,
                        )
                    }
                }
            }
        }
    }

    // ── Goal name ─────────────────────────────────────────────────────────────
    FormCard {
        Text(stringResource(R.string.addgoal_name), color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value         = form.name,
            onValueChange = actions.onNameChanged,
            placeholder   = { Text(stringResource(R.string.addgoal_name_hint), fontFamily = AppUiFontFamily) },
            modifier      = Modifier.fillMaxWidth(),
            singleLine    = true,
            colors        = outlinedFieldColors(),
            shape         = RoundedCornerShape(8.dp),
        )
    }

    // ── Category ──────────────────────────────────────────────────────────────
    FormCard {
        Text(stringResource(R.string.addgoal_category), color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            GoalCategory.values().forEach { category ->
                CategoryOption(category, isSelected = category == form.category, onClick = { actions.onCategoryChanged(category) })
            }
        }
    }

    // ── Frequency + target (+ weekdays for daily goals) ───────────────────────
    FormCard {
        Text(stringResource(R.string.addgoal_target_freq), color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily)
        Spacer(Modifier.height(12.dp))
        FrequencyTabs(selected = form.frequency, onSelected = actions.onFrequencyChanged)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.addgoal_amount), color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = form.targetAmount,
                    onValueChange = actions.onTargetAmountChanged,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = outlinedFieldColors(),
                    shape = RoundedCornerShape(8.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.addgoal_unit), color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily)
                Spacer(Modifier.height(6.dp))
                UnitDropdown(selected = form.unit, onSelected = actions.onUnitChanged)
            }
        }

        if (form.frequency == GoalFrequency.DAILY) {
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.addgoal_repeat_on), color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily)
            Spacer(Modifier.height(8.dp))
            WeekdayChips(
                selected = form.daysOfWeek,
                onDayToggled = actions.onDayToggled,
                onEveryDaySelected = actions.onEveryDaySelected,
            )
        }
    }

    if (form.category == GoalCategory.DHIKR && form.unit == "Time(s)") {
        FormCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.tasbih_goal_switch), color = SalatOnSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily)
                    Spacer(Modifier.height(3.dp))
                    Text(stringResource(R.string.tasbih_goal_switch_desc), color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily)
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = form.usesTasbihCounter,
                    onCheckedChange = actions.onUsesTasbihCounterChanged,
                    colors = salatSwitchColors(),
                )
            }
        }
    }

    // ── Reminder ──────────────────────────────────────────────────────────────
    FormCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(painterResource(R.drawable.ic_schedule), null, tint = SalatPrimary, modifier = Modifier.size(20.dp))
                Text(stringResource(R.string.addgoal_reminder), color = SalatOnSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily)
            }
            Switch(
                checked = form.reminderEnabled,
                onCheckedChange = actions.onReminderEnabledChanged,
                colors = salatSwitchColors(),
            )
        }
        if (form.reminderEnabled) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.addgoal_notify_at), color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily)
                Surface(onClick = { showTimePicker = true }, shape = RoundedCornerShape(8.dp), color = SalatSurfaceContainerLow) {
                    Text(
                        text = form.reminderTime.formatAppTime(use24HourFormat),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = SalatPrimaryContainer, fontWeight = FontWeight.SemiBold, fontFamily = AppTitleFontFamily,
                    )
                }
            }
        }
    }

    // ── End after N days ──────────────────────────────────────────────────────
    FormCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.addgoal_end_after), color = SalatOnSurface, fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily, modifier = Modifier.weight(1f),
            )
            Switch(
                checked = form.endsAfterDuration,
                onCheckedChange = actions.onEndsAfterChanged,
                colors = salatSwitchColors(),
            )
        }
        if (form.endsAfterDuration) {
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.addgoal_duration_days), color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = form.durationDays,
                onValueChange = actions.onDurationDaysChanged,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = outlinedFieldColors(),
                shape = RoundedCornerShape(8.dp),
            )
        }
    }

    if (showTimePicker) {
        TimePickerModal(
            initialTime = form.reminderTime,
            use24HourFormat = use24HourFormat,
            onTimePicked = { picked ->
                actions.onReminderTimeChanged(picked)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
        )
    }
}

@Composable
private fun WeekdayChips(
    selected: Set<DayOfWeek>,
    onDayToggled: (DayOfWeek) -> Unit,
    onEveryDaySelected: () -> Unit,
) {
    val everyDay = selected.isEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            onClick = onEveryDaySelected,
            shape = RoundedCornerShape(50),
            color = if (everyDay) SalatPrimaryContainer else SalatSurfaceContainerLow,
            border = BorderStroke(1.dp, if (everyDay) SalatPrimaryContainer else SalatOutlineVariant),
        ) {
            Text(
                stringResource(R.string.addgoal_every_day), modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                color = if (everyDay) SalatOnPrimaryContainer else SalatOnSurfaceVariant,
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily,
            )
        }
        Row(Modifier.fillMaxWidth()) {
            SUNDAY_FIRST.forEach { day ->
                val isSelected = day in selected
                // weight(1f) so 7 columns always fit the card width, however narrow the screen —
                // a fixed-width circle per day here previously overflowed and got clipped.
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Surface(
                        onClick = { onDayToggled(day) },
                        shape = CircleShape,
                        color = if (isSelected) SalatPrimaryContainer else Color.Transparent,
                        border = BorderStroke(1.dp, if (isSelected) SalatPrimaryContainer else SalatOutlineVariant),
                    ) {
                        Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                            Text(
                                day.shortLabel(), color = if (isSelected) SalatOnPrimaryContainer else SalatSecondary,
                                 fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily, maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}
