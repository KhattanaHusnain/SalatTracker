package com.salattracker.domain.usecase

import com.salattracker.domain.model.GeofenceSync
import com.salattracker.domain.model.Mosque
import com.salattracker.domain.model.MosqueModeState
import com.salattracker.domain.model.RingerMode
import com.salattracker.testutil.FakeMosqueGeofencer
import com.salattracker.testutil.FakeMosqueModeNotifier
import com.salattracker.testutil.FakeMosqueModeStore
import com.salattracker.testutil.FakeMosqueRepository
import com.salattracker.testutil.FakeRingerController
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MosqueModeControllerTest {

    private val alNoor = Mosque(1, "Al-Noor", 24.0, 39.0)
    private val jamia = Mosque(2, "Jamia", 24.001, 39.001)

    private val repository = FakeMosqueRepository(listOf(alNoor, jamia))
    private val store = FakeMosqueModeStore(MosqueModeState(enabled = true))
    private val geofencer = FakeMosqueGeofencer()
    private val ringer = FakeRingerController()
    private val notifier = FakeMosqueModeNotifier()
    private val controller = MosqueModeController(repository, store, geofencer, ringer, notifier)

    @Test
    fun `dwell in a mosque switches a normal ringer to vibrate and posts the notification`() = runTest {
        controller.onDwell(listOf(1))

        assertEquals(RingerMode.VIBRATE, ringer.mode)
        assertEquals(listOf("Al-Noor"), notifier.shown)
        val state = store.getState()
        assertEquals(setOf(1L), state.insideIds)
        assertEquals(RingerMode.NORMAL, state.restoreTo)
        assertEquals("Al-Noor", state.activeMosqueName)
    }

    @Test
    fun `dwell leaves a ringer that is already on vibrate or silent alone`() = runTest {
        ringer.mode = RingerMode.VIBRATE
        controller.onDwell(listOf(1))
        controller.onExit(listOf(1))
        ringer.mode = RingerMode.SILENT
        controller.onDwell(listOf(1))

        assertTrue(ringer.setCalls.isEmpty())
        assertTrue(notifier.shown.isEmpty())
        assertEquals(RingerMode.SILENT, ringer.mode)
        assertNull(store.getState().restoreTo)
    }

    @Test
    fun `exit restores the sound and clears the visit`() = runTest {
        controller.onDwell(listOf(1))
        controller.onExit(listOf(1))

        assertEquals(RingerMode.NORMAL, ringer.mode)
        assertEquals(1, notifier.dismissCount)
        val state = store.getState()
        assertTrue(state.insideIds.isEmpty())
        assertNull(state.restoreTo)
    }

    @Test
    fun `exit keeps the ringer the user chose while inside`() = runTest {
        controller.onDwell(listOf(1))
        ringer.mode = RingerMode.SILENT
        controller.onExit(listOf(1))

        assertEquals(RingerMode.SILENT, ringer.mode)
        assertEquals(listOf(RingerMode.VIBRATE), ringer.setCalls)
        assertEquals(1, notifier.dismissCount)
        assertTrue(store.getState().insideIds.isEmpty())
    }

    @Test
    fun `overlapping mosques only restore once the phone is outside all of them`() = runTest {
        controller.onDwell(listOf(1))
        controller.onDwell(listOf(2))
        assertEquals(listOf(RingerMode.VIBRATE), ringer.setCalls)
        assertEquals(1, notifier.shown.size)

        controller.onExit(listOf(1))
        assertEquals(RingerMode.VIBRATE, ringer.mode)
        assertEquals(setOf(2L), store.getState().insideIds)

        controller.onExit(listOf(2))
        assertEquals(RingerMode.NORMAL, ringer.mode)
    }

    @Test
    fun `restore sound keeps the phone marked inside so the same visit does not vibrate again`() = runTest {
        controller.onDwell(listOf(1))
        controller.restoreSound()

        assertEquals(RingerMode.NORMAL, ringer.mode)
        assertEquals(1, notifier.dismissCount)
        assertEquals(setOf(1L), store.getState().insideIds)
        assertNull(store.getState().restoreTo)

        controller.onDwell(listOf(1))
        assertEquals(RingerMode.NORMAL, ringer.mode)
        assertEquals(1, notifier.shown.size)

        controller.onExit(listOf(1))
        controller.onDwell(listOf(1))
        assertEquals(RingerMode.VIBRATE, ringer.mode)
        assertEquals(2, notifier.shown.size)
    }

    @Test
    fun `disabling while inside restores the sound and clears the geofences`() = runTest {
        controller.onDwell(listOf(1))
        val result = controller.setEnabled(false)

        assertEquals(GeofenceSync.OK, result)
        assertEquals(RingerMode.NORMAL, ringer.mode)
        assertFalse(store.getState().enabled)
        assertTrue(store.getState().insideIds.isEmpty())
        assertEquals(1, geofencer.clearCount)
    }

    @Test
    fun `deleting the mosque you are in ends the visit on the next sync`() = runTest {
        controller.onDwell(listOf(1))
        repository.deleteMosque(1)
        controller.sync()

        assertEquals(RingerMode.NORMAL, ringer.mode)
        assertTrue(store.getState().insideIds.isEmpty())
        assertEquals(listOf(jamia), geofencer.registered.last())
    }

    @Test
    fun `deleting one of two overlapping mosques keeps the visit going`() = runTest {
        controller.onDwell(listOf(1))
        controller.onDwell(listOf(2))
        repository.deleteMosque(1)
        controller.sync()

        assertEquals(RingerMode.VIBRATE, ringer.mode)
        assertEquals(setOf(2L), store.getState().insideIds)
    }

    @Test
    fun `an exit reported after a missed one ends a stale visit and restores the sound`() = runTest {
        store.setVisit(setOf(1), RingerMode.NORMAL, "Al-Noor")
        ringer.mode = RingerMode.VIBRATE

        controller.onExit(listOf(1))

        assertEquals(RingerMode.NORMAL, ringer.mode)
        assertTrue(store.getState().insideIds.isEmpty())
    }

    @Test
    fun `events for unknown mosques are ignored`() = runTest {
        controller.onDwell(listOf(99))
        controller.onExit(listOf(99))

        assertTrue(ringer.setCalls.isEmpty())
        assertTrue(store.getState().insideIds.isEmpty())
    }

    @Test
    fun `dwell events are ignored while the feature is off`() = runTest {
        store.setEnabled(false)
        controller.onDwell(listOf(1))

        assertTrue(ringer.setCalls.isEmpty())
        assertTrue(notifier.shown.isEmpty())
    }

    @Test
    fun `a device without a vibrator is never switched`() = runTest {
        ringer.canVibrate = false
        controller.onDwell(listOf(1))

        assertTrue(ringer.setCalls.isEmpty())
        assertEquals(RingerMode.NORMAL, ringer.mode)
    }

    @Test
    fun `a ringer change that is refused leaves no stale restore target or notification`() = runTest {
        ringer.acceptChanges = false
        controller.onDwell(listOf(1))

        assertTrue(notifier.shown.isEmpty())
        assertNull(store.getState().restoreTo)
        assertEquals(setOf(1L), store.getState().insideIds)
    }

    @Test
    fun `sync registers every saved mosque when enabled`() = runTest {
        val result = controller.sync()

        assertEquals(GeofenceSync.OK, result)
        assertEquals(listOf(alNoor, jamia), geofencer.registered.last())
        assertEquals(0, geofencer.clearCount)
    }

    @Test
    fun `sync passes the geofencer's failure back to the caller`() = runTest {
        geofencer.result = GeofenceSync.LOCATION_OFF

        assertEquals(GeofenceSync.LOCATION_OFF, controller.sync())
    }

    @Test
    fun `sync clears geofences when disabled, empty, or the device cannot vibrate`() = runTest {
        store.setEnabled(false)
        controller.sync()
        assertEquals(1, geofencer.clearCount)

        store.setEnabled(true)
        ringer.canVibrate = false
        controller.sync()
        assertEquals(2, geofencer.clearCount)

        ringer.canVibrate = true
        repository.deleteMosque(1)
        repository.deleteMosque(2)
        controller.sync()
        assertEquals(3, geofencer.clearCount)
        assertTrue(geofencer.registered.isEmpty())
    }
}
