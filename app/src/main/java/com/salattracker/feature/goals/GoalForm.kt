package com.salattracker.feature.goals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.AppUiFontFamily
import com.salattracker.core.theme.SalatOnSurface
import com.salattracker.core.theme.SalatOnSurfaceVariant
import com.salattracker.core.theme.SalatOutlineVariant
import com.salattracker.core.theme.SalatPrimary
import com.salattracker.core.theme.SalatSurface
import com.salattracker.core.ui.formatAppTime
import com.salattracker.core.ui.goalUnitLabel
import com.salattracker.core.ui.iconRes
import com.salattracker.core.ui.salatSwitchColors
import com.salattracker.core.ui.shortLabel
import com.salattracker.domain.model.GoalCategory
import com.salattracker.domain.model.GoalFrequency
import java.time.DayOfWeek
import java.time.LocalTime

val GOAL_UNITS = listOf("Time(s)", "Minutes", "Pages", "Rak'ahs")
internal const val MAX_LOG_AMOUNT = 9999

enum class GoalTemplate(
    val nameRes: Int,
    val category: GoalCategory,
    val frequency: GoalFrequency,
    val target: Int,
    val unit: String,
    val days: Set<DayOfWeek> = emptySet(),
) {
    READ_QURAN(R.string.template_read_quran, GoalCategory.QURAN, GoalFrequency.DAILY, 1, "Pages"),
    MORNING_ADHKAR(R.string.template_morning_adhkar, GoalCategory.DHIKR, GoalFrequency.DAILY, 1, "Time(s)"),
    EVENING_ADHKAR(R.string.template_evening_adhkar, GoalCategory.DHIKR, GoalFrequency.DAILY, 1, "Time(s)"),
    FAST_MON_THU(R.string.template_fast_mon_thu, GoalCategory.FASTING, GoalFrequency.DAILY, 1, "Time(s)", setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)),
    AL_KAHF_FRIDAY(R.string.template_al_kahf, GoalCategory.QURAN, GoalFrequency.DAILY, 1, "Time(s)", setOf(DayOfWeek.FRIDAY)),
    TAHAJJUD(R.string.template_tahajjud, GoalCategory.SALAT, GoalFrequency.DAILY, 2, "Rak'ahs"),
    WEEKLY_SADAQAH(R.string.template_sadaqah, GoalCategory.CHARITY, GoalFrequency.WEEKLY, 1, "Time(s)"),
    TASBIH(R.string.template_tasbih, GoalCategory.DHIKR, GoalFrequency.DAILY, 1, "Time(s)"),
}

internal val PRIMARY_GOAL_TEMPLATES = listOf(
    GoalTemplate.READ_QURAN,
    GoalTemplate.MORNING_ADHKAR,
    GoalTemplate.TAHAJJUD,
    GoalTemplate.FAST_MON_THU,
    GoalTemplate.WEEKLY_SADAQAH,
)

internal val ADVANCED_GOAL_TEMPLATES = listOf(
    GoalTemplate.EVENING_ADHKAR,
    GoalTemplate.AL_KAHF_FRIDAY,
    GoalTemplate.TASBIH,
)

data class GoalFormState(
    val name: String = "",
    val category: GoalCategory = GoalCategory.SALAT,
    val frequency: GoalFrequency = GoalFrequency.DAILY,
    val targetAmount: String = "1",
    val unit: String = GOAL_UNITS.first(),
    val reminderEnabled: Boolean = true,
    val reminderTime: LocalTime = LocalTime.of(4, 30),
    val daysOfWeek: Set<DayOfWeek> = emptySet(),
    val endsAfterDuration: Boolean = false,
    val durationDays: String = "30",
    val usesTasbihCounter: Boolean = false,
    val selectedTemplate: GoalTemplate? = null,
) {
    val amount: Int get() = targetAmount.toIntOrNull()?.takeIf { it > 0 } ?: 1
    val duration: Int get() = durationDays.toIntOrNull()?.takeIf { it > 0 } ?: 30
    val effectiveDays: Set<DayOfWeek> get() =
        if (frequency == GoalFrequency.DAILY && daysOfWeek.size < 7) daysOfWeek else emptySet()

    fun withTargetAmount(value: String) = if (value.all(Char::isDigit)) copy(targetAmount = value) else this
    fun withDurationDays(value: String) = if (value.all(Char::isDigit)) copy(durationDays = value) else this
    fun withDayToggled(day: DayOfWeek) = copy(daysOfWeek = if (day in daysOfWeek) daysOfWeek - day else daysOfWeek + day)
    fun withTemplate(template: GoalTemplate, localizedName: String) = copy(
        name = localizedName,
        category = template.category,
        frequency = template.frequency,
        targetAmount = template.target.toString(),
        unit = template.unit,
        daysOfWeek = template.days,
        usesTasbihCounter = template == GoalTemplate.TASBIH,
        selectedTemplate = template,
    )
}

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

@Composable
fun GoalFormFields(
    form: GoalFormState,
    actions: GoalFormActions,
    use24HourFormat: Boolean,
    showTemplates: Boolean,
) {
    var showTimePicker by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(!showTemplates) }

    if (showTemplates) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            PRIMARY_GOAL_TEMPLATES.forEach { template ->
                val selected = form.selectedTemplate == template
                val label = stringResource(template.nameRes)
                Text(
                    text = label,
                    color = if (selected) SalatPrimary else SalatOnSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    fontFamily = AppUiFontFamily,
                    modifier = Modifier
                        .clickable { actions.onTemplateSelected?.invoke(template, label) }
                        .padding(horizontal = 10.dp, vertical = 11.dp),
                )
            }
        }
    }

    FlatLabel(stringResource(R.string.addgoal_name))
    TextField(
        value = form.name,
        onValueChange = actions.onNameChanged,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.addgoal_name_hint), fontFamily = AppUiFontFamily) },
        singleLine = true,
        colors = flatTextFieldColors(),
    )

    FlatLabel(stringResource(R.string.goals_v2_target))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        GoalStepper(
            value = form.amount,
            onMinus = { actions.onTargetAmountChanged((form.amount - 1).coerceAtLeast(1).toString()) },
            onPlus = { actions.onTargetAmountChanged((form.amount + 1).coerceAtMost(MAX_LOG_AMOUNT).toString()) },
        )
        GoalUnitDropdown(form.unit, actions.onUnitChanged, Modifier.weight(1f))
    }

    FlatLabel(stringResource(R.string.goals_v2_how_often))
    Row(Modifier.fillMaxWidth()) {
        GoalFrequency.entries.forEach { frequency ->
            val selected = form.frequency == frequency
            Text(
                text = frequencyLabel(frequency),
                color = if (selected) SalatPrimary else SalatOnSurfaceVariant,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                fontFamily = AppUiFontFamily,
                fontSize = 13.sp,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .clickable { actions.onFrequencyChanged(frequency) }
                    .padding(vertical = 11.dp),
            )
        }
    }
    HorizontalDivider(color = SalatOutlineVariant)

    if (form.frequency == GoalFrequency.DAILY) {
        FlatLabel(stringResource(R.string.goals_v2_on_days))
        WeekdaySelector(form.daysOfWeek, actions.onDayToggled, actions.onEveryDaySelected)
    }

    FlatSwitchRow(stringResource(R.string.goals_v2_remind_me), form.reminderEnabled, actions.onReminderEnabledChanged)
    if (form.reminderEnabled) {
        Text(
            form.reminderTime.formatAppTime(use24HourFormat),
            color = SalatPrimary,
            fontSize = 16.sp,
            fontFamily = AppUiFontFamily,
            modifier = Modifier.fillMaxWidth().clickable { showTimePicker = true }.padding(vertical = 12.dp),
        )
        HorizontalDivider(color = SalatOutlineVariant)
    }

    FlatSwitchRow(stringResource(R.string.goals_v2_stop_after), form.endsAfterDuration, actions.onEndsAfterChanged)
    if (form.endsAfterDuration) {
        FlatLabel(stringResource(R.string.addgoal_duration_days))
        TextField(
            value = form.durationDays,
            onValueChange = actions.onDurationDaysChanged,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = flatTextFieldColors(),
        )
    }

    TextButton(onClick = { showMore = !showMore }, modifier = Modifier.padding(top = 2.dp)) {
        Text(stringResource(R.string.goals_v2_more_options), color = SalatPrimary, fontFamily = AppUiFontFamily)
        Icon(painterResource(if (showMore) R.drawable.ic_expand_less else R.drawable.ic_expand_more), null, tint = SalatPrimary)
    }
    if (showMore) {
        AdvancedGoalOptions(form, actions, showTemplates)
    }

    if (showTimePicker) {
        TimePickerModal(
            initialTime = form.reminderTime,
            use24HourFormat = use24HourFormat,
            onTimePicked = { actions.onReminderTimeChanged(it); showTimePicker = false },
            onDismiss = { showTimePicker = false },
        )
    }
}

@Composable
private fun AdvancedGoalOptions(form: GoalFormState, actions: GoalFormActions, showTemplates: Boolean) {
    if (showTemplates) {
        FlatLabel(stringResource(R.string.goals_v2_advanced_presets))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            ADVANCED_GOAL_TEMPLATES.forEach { template ->
                val label = stringResource(template.nameRes)
                TextButton(onClick = { actions.onTemplateSelected?.invoke(template, label) }) {
                    Text(label, color = SalatPrimary, fontFamily = AppUiFontFamily)
                }
            }
        }
    }
    FlatLabel(stringResource(R.string.addgoal_category))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        GoalCategory.entries.forEach { category ->
            val selected = category == form.category
            IconButton(onClick = { actions.onCategoryChanged(category) }) {
                Icon(
                    painterResource(category.iconRes),
                    contentDescription = category.name,
                    tint = if (selected) SalatPrimary else SalatOnSurfaceVariant,
                )
            }
        }
    }
    if (form.category == GoalCategory.DHIKR && form.unit == "Time(s)") {
        FlatSwitchRow(stringResource(R.string.tasbih_goal_switch), form.usesTasbihCounter, actions.onUsesTasbihCounterChanged)
        Text(stringResource(R.string.tasbih_goal_switch_desc), color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily)
    }
}

@Composable
private fun GoalStepper(value: Int, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StepButton("−", onMinus, value > 1)
        Text(value.toString(), color = SalatOnSurface, fontSize = 20.sp, fontWeight = FontWeight.Medium, fontFamily = AppUiFontFamily)
        StepButton("+", onPlus, true)
    }
}

@Composable
internal fun StepButton(text: String, onClick: () -> Unit, enabled: Boolean) {
    val iconRes = if (text == "+") R.drawable.ic_add else R.drawable.ic_remove
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(if (enabled) SalatPrimary.copy(alpha = .16f) else SalatOutlineVariant.copy(alpha = .35f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = text,
            tint = if (enabled) SalatPrimary else SalatOnSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalUnitDropdown(selected: String, onSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        TextField(
            value = goalUnitLabel(selected),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            colors = flatTextFieldColors(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            GOAL_UNITS.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(goalUnitLabel(unit), fontFamily = AppUiFontFamily) },
                    onClick = { onSelected(unit); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun WeekdaySelector(selected: Set<DayOfWeek>, onToggle: (DayOfWeek) -> Unit, onEveryDay: () -> Unit) {
    val mondayFirst = DayOfWeek.entries
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        mondayFirst.forEach { day ->
            val on = selected.isEmpty() || day in selected
            Text(
                day.shortLabel(),
                color = if (on) SalatPrimary else SalatOnSurfaceVariant,
                fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                fontFamily = AppUiFontFamily,
                fontSize = 13.sp,
                modifier = Modifier.clickable {
                    if (selected.isEmpty()) {
                        onToggle(day)
                    } else if (selected.size == 1 && day in selected) {
                        onEveryDay()
                    } else onToggle(day)
                }.padding(10.dp),
            )
        }
    }
}

@Composable
private fun FlatSwitchRow(label: String, checked: Boolean, onChanged: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(54.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = SalatOnSurface, fontFamily = AppUiFontFamily, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChanged, colors = salatSwitchColors())
    }
    HorizontalDivider(color = SalatOutlineVariant)
}

@Composable
private fun FlatLabel(text: String) {
    Text(text, color = SalatOnSurfaceVariant, fontSize = 12.5.sp, fontFamily = AppUiFontFamily, modifier = Modifier.padding(top = 14.dp, bottom = 2.dp))
}

@Composable
private fun frequencyLabel(frequency: GoalFrequency): String = stringResource(
    when (frequency) {
        GoalFrequency.DAILY -> R.string.goals_v2_every_day
        GoalFrequency.WEEKLY -> R.string.goals_v2_every_week
        GoalFrequency.MONTHLY -> R.string.goals_v2_every_month
    }
)

@Composable
private fun flatTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    focusedIndicatorColor = SalatPrimary,
    unfocusedIndicatorColor = SalatOutlineVariant,
    focusedTextColor = SalatOnSurface,
    unfocusedTextColor = SalatOnSurface,
)
