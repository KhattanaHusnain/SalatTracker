package com.salattracker.feature.developers

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.salattracker.R

@Composable
fun DevelopersEntry() {
    val context = LocalContext.current
    DevelopersScreen(
        onLinkedInClick = { url -> context.openLinkedIn(url) },
        onContributeClick = { context.sendContributionEnquiry() },
    )
}

private fun Context.openLinkedIn(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        // No browser or LinkedIn app available; nothing we can do here.
    }
}

private fun Context.sendContributionEnquiry() {
    val subject = getString(R.string.developer_contribute_email_subject)
    val body = getString(R.string.developer_contribute_email_body)
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$DEVELOPER_EMAIL")).apply {
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // No email app installed; nothing we can do here.
    }
}

private const val DEVELOPER_EMAIL = "mujahid.husnain.56@gmail.com"
