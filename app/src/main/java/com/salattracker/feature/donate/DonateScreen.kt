package com.salattracker.feature.donate

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.AppUiFontFamily
import com.salattracker.core.theme.AppTitleFontFamily
import com.salattracker.core.theme.SalatOnPrimary
import com.salattracker.core.theme.SalatOnSurface
import com.salattracker.core.theme.SalatOnSurfaceVariant
import com.salattracker.core.theme.SalatOutlineVariant
import com.salattracker.core.theme.SalatPrimary
import com.salattracker.core.theme.SalatSecondaryContainer
import com.salattracker.core.ui.SettingsCard

private const val ACCOUNT_TITLE = "MUJAHID HUSNAIN"
private const val BANK_NAME = "MCB Bank"
private const val ACCOUNT_NUMBER = "1186888311019025"
private const val IBAN = "PK20MUCB1186888311019025"
private const val WALLET_NUMBER = "03130781581"
private val WALLET_PROVIDERS = listOf("Easypaisa", "JazzCash", "NayaPay", "RAAST ID")

@Composable
fun DonateScreen(onContactClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SettingsCard {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    painterResource(R.drawable.ic_heart),
                    contentDescription = null,
                    tint = SalatPrimary,
                    modifier = Modifier.size(32.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.donate_title),
                    color = SalatPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = AppTitleFontFamily,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.donate_subtitle),
                    color = SalatOnSurfaceVariant,
                    fontSize = 13.sp,
                    fontFamily = AppUiFontFamily,
                )
            }
        }
        SettingsCard {
            Text(
                stringResource(R.string.donate_body),
                color = SalatOnSurface,
                fontSize = 14.sp,
                fontFamily = AppUiFontFamily,
                lineHeight = 20.sp,
            )
        }
        SettingsCard {
            SectionHeading(stringResource(R.string.donate_bank_title), BANK_NAME)
            Spacer(Modifier.height(10.dp))
            CopyableRow(stringResource(R.string.donate_account_title_label), ACCOUNT_TITLE)
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = SalatOutlineVariant)
            Spacer(Modifier.height(10.dp))
            CopyableRow(stringResource(R.string.donate_account_number_label), ACCOUNT_NUMBER)
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = SalatOutlineVariant)
            Spacer(Modifier.height(10.dp))
            CopyableRow(stringResource(R.string.donate_iban_label), IBAN)
        }
        SettingsCard {
            SectionHeading(stringResource(R.string.donate_wallets_title), null)
            Spacer(Modifier.height(10.dp))
            CopyableRow(stringResource(R.string.donate_account_title_label), ACCOUNT_TITLE)
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = SalatOutlineVariant)
            Spacer(Modifier.height(10.dp))
            CopyableRow(stringResource(R.string.donate_wallet_number_label), WALLET_NUMBER)
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.donate_wallet_available_label),
                color = SalatOnSurfaceVariant,
                fontSize = 12.sp,
                fontFamily = AppUiFontFamily,
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WALLET_PROVIDERS.forEach { provider -> ProviderChip(provider) }
            }
        }
        SettingsCard {
            Text(
                stringResource(R.string.donate_contact_hint),
                color = SalatOnSurfaceVariant,
                fontSize = 13.sp,
                fontFamily = AppUiFontFamily,
            )
            Spacer(Modifier.height(12.dp))
            com.salattracker.core.ui.SalatPrimaryButton(
                onClick = onContactClick,
            ) {
                Text(stringResource(R.string.donate_contact_button), fontFamily = AppUiFontFamily, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun SectionHeading(title: String, trailing: String?) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = SalatPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily)
        if (trailing != null) {
            Text(trailing, color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily)
        }
    }
}

@Composable
private fun CopyableRow(label: String, value: String) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val copiedMessage = stringResource(R.string.donate_copied)
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily)
            Text(value, color = SalatOnSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily)
        }
        IconButton(onClick = {
            clipboardManager.setText(AnnotatedString(value))
            Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
        }) {
            Icon(
                painterResource(R.drawable.ic_copy),
                contentDescription = stringResource(R.string.donate_copy),
                tint = SalatPrimary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ProviderChip(label: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = SalatSecondaryContainer) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            color = SalatOnSurfaceVariant,
             fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = AppUiFontFamily,
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DonateScreenPreview() {
    DonateScreen(onContactClick = {})
}
