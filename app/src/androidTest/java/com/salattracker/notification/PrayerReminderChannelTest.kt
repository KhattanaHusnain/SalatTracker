package com.salattracker.notification

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.FileNotFoundException
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PrayerReminderChannelTest {
    @Test
    fun bundledAzanUsesStableResolvableResourceNameUri() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val channels = PrayerReminderChannels(context)
        val sound = channels.azanUri()
        assertEquals("android.resource://${context.packageName}/raw/azan", sound.toString())
        try {
            context.contentResolver.openAssetFileDescriptor(sound, "r")?.use { descriptor ->
                if (descriptor.length == 0L) fail("Bundled Azan resource is empty")
            } ?: fail("Bundled Azan resource could not be opened")
        } catch (error: FileNotFoundException) {
            fail("Bundled Azan URI did not resolve: ${error.message}")
        }
    }
}
