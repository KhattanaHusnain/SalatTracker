package com.salattracker.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.domain.model.CityResult

/**
 * Shared city-search text field, used by onboarding's City Search step and Settings' Edit
 * Location screen so both get the same look and the same debounced-search contract (the caller
 * drives the actual [GeocodingRepository][com.salattracker.domain.repository.GeocodingRepository]
 * call and passes back [query]/results — this is UI only).
 */
@Composable
fun CitySearchField(query: String, onQueryChanged: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.onboarding_city_search_placeholder), fontFamily = AppUiFontFamily) },
        leadingIcon = { Icon(painterResource(R.drawable.ic_search), null, tint = SalatOnSurfaceVariant) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        textStyle = LocalTextStyle.current.copy(fontFamily = AppUiFontFamily, fontSize = 15.sp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SalatPrimary, unfocusedBorderColor = SalatOutlineVariant,
            focusedContainerColor = SalatSurfaceContainerLow, unfocusedContainerColor = SalatSurfaceContainerLow,
        ),
        shape = RoundedCornerShape(14.dp),
    )
}

/** The searching-spinner / no-results / result-list states below [CitySearchField]. */
@Composable
fun CitySearchResults(
    query: String,
    results: List<CityResult>,
    isSearching: Boolean,
    selected: CityResult?,
    onSelect: (CityResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        isSearching -> Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = SalatPrimary)
            Text(stringResource(R.string.onboarding_city_search_searching), color = SalatOnSurfaceVariant, fontSize = 13.sp, fontFamily = AppUiFontFamily)
        }
        query.isNotBlank() && results.isEmpty() -> Text(
            stringResource(R.string.onboarding_city_search_no_results, query),
            modifier = modifier,
            color = SalatOnSurfaceVariant, fontSize = 13.sp, fontFamily = AppUiFontFamily, lineHeight = 18.sp,
        )
        else -> Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            results.forEach { city ->
                CityResultRow(city = city, isSelected = city == selected, onClick = { onSelect(city) })
            }
        }
    }
}

@Composable
private fun CityResultRow(city: CityResult, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        color = if (isSelected) SalatPrimaryContainer.copy(alpha = 0.4f) else SalatBackground,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(city.name, color = SalatOnSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily)
                if (city.displayLine.isNotBlank()) {
                    Text(city.displayLine, color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily)
                }
            }
            if (isSelected) {
                Icon(painterResource(R.drawable.ic_check_circle_filled), null, tint = SalatPrimary, modifier = Modifier.size(20.dp))
            }
        }
    }
}
