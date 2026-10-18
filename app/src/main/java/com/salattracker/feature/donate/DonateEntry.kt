package com.salattracker.feature.donate

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.salattracker.R

@Composable
fun DonateEntry() {
    val context = LocalContext.current
    DonateScreen(onContactClick = { context.sendDonationEnquiry() })
}

private fun Context.sendDonationEnquiry() {
    val subject = getString(R.string.donate_email_subject)
    val body = getString(R.string.donate_email_body)
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
