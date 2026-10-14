package com.salattracker.core.ui

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.salattracker.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Shared Material3 date-picker dialog.
 *
 * [earliestSelectableDate] / [latestSelectableDate] bound the pickable range; either may be
 * null for no bound on that side. Defaults to "no earlier bound, nothing after today" — the
 * shape every caller before this one needed (Bulk Update's custom range).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(
    initialDate: LocalDate,
    onDatePicked: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    earliestSelectableDate: LocalDate? = null,
    latestSelectableDate: LocalDate? = LocalDate.now(),
) {
    val initialMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val earliestMillis = earliestSelectableDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    val latestMillis = latestSelectableDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()

    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        selectableDates = remember(earliestMillis, latestMillis) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    (earliestMillis == null || utcTimeMillis >= earliestMillis) &&
                        (latestMillis == null || utcTimeMillis <= latestMillis)
            }
        },
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = state.selectedDateMillis
                if (millis != null) {
                    val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    onDatePicked(picked)
                } else {
                    onDismiss()
                }
            }) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    ) {
        DatePicker(state = state)
    }
}
