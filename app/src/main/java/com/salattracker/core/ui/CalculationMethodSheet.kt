package com.salattracker.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.domain.model.CalculationMethod

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculationMethodSheet(selected: CalculationMethod, onSelect: (CalculationMethod) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SalatSurfaceContainerLowest) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 12.dp)) {
            Text(stringResource(R.string.settings_calculation_method_sheet_title), color = SalatPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppTitleFontFamily)
            Spacer(Modifier.height(12.dp))
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(CalculationMethod.values()) { method ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onSelect(method) }.padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val selectedMethod = method == selected
                        Text(method.displayName, Modifier.weight(1f), if (selectedMethod) SalatPrimary else SalatOnSurface, 14.sp, if (selectedMethod) FontWeight.SemiBold else FontWeight.Normal, fontFamily = AppUiFontFamily)
                        if (selectedMethod) Icon(painterResource(R.drawable.ic_check_circle_filled), null, tint = SalatPrimary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
