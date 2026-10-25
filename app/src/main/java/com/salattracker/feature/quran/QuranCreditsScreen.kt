package com.salattracker.feature.quran

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.AppUiFontFamily
import com.salattracker.core.theme.SalatOnSurface
import com.salattracker.core.theme.SalatOnSurfaceVariant
import com.salattracker.core.theme.SalatOutlineVariant
import com.salattracker.core.theme.SalatPrimary
import com.salattracker.core.ui.SettingsCard

/**
 * Attribution owed to each of this feature's content sources, per their own stated reuse terms —
 * see QURAN_CONTENT_SOURCES.md for the full research behind every entry here. This screen exists
 * specifically to fulfil those terms (e.g. Tanzil's "its source must be clearly indicated",
 * QuranEnc's "clearly referring to the publisher"), not just as a nice-to-have about page.
 */
private data class QuranCredit(val titleRes: Int, val bodyRes: Int, val url: String)

private val QURAN_CREDITS = listOf(
    QuranCredit(R.string.quran_credits_arabic_title, R.string.quran_credits_arabic_body, "https://tanzil.net"),
    QuranCredit(R.string.quran_credits_translation_title, R.string.quran_credits_translation_body, "https://alquran.cloud"),
    QuranCredit(R.string.quran_credits_mushaf_title, R.string.quran_credits_mushaf_body, "https://quran.foundation"),
    QuranCredit(R.string.quran_credits_amiri_title, R.string.quran_credits_amiri_body, "https://github.com/aliftype/amiri"),
    QuranCredit(R.string.quran_credits_nastaliq_title, R.string.quran_credits_nastaliq_body, "https://github.com/notofonts/NotoNastaliqUrdu"),
)

@Composable
fun QuranCreditsScreen(onSourceClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            stringResource(R.string.quran_credits_intro),
            color = SalatOnSurfaceVariant, fontSize = 13.sp, fontFamily = AppUiFontFamily, lineHeight = 19.sp,
        )
        QURAN_CREDITS.forEach { credit ->
            QuranCreditCard(credit = credit, onSourceClick = { onSourceClick(credit.url) })
        }
    }
}

@Composable
private fun QuranCreditCard(credit: QuranCredit, onSourceClick: () -> Unit) {
    SettingsCard {
        Text(
            stringResource(credit.titleRes),
            color = SalatPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(credit.bodyRes),
            color = SalatOnSurface, fontSize = 13.sp, fontFamily = AppUiFontFamily, lineHeight = 19.sp,
        )
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = SalatOutlineVariant)
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onSourceClick)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(painterResource(R.drawable.ic_external_link), contentDescription = null, tint = SalatPrimary, modifier = Modifier.size(20.dp))
            Text(
                stringResource(R.string.quran_credits_visit_source),
                color = SalatPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFamily = AppUiFontFamily,
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun QuranCreditsScreenPreview() {
    QuranCreditsScreen(onSourceClick = {})
}
