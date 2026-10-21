package com.salattracker.feature.quran

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun QuranCreditsEntry() {
    val context = LocalContext.current
    QuranCreditsScreen(onSourceClick = { url -> context.openUrl(url) })
}

private fun Context.openUrl(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        // No browser available; nothing we can do here.
    }
}
