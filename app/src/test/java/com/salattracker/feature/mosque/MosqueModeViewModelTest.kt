package com.salattracker.feature.mosque

import com.salattracker.domain.model.GeofenceSync
import com.salattracker.domain.model.LocationAccess
import com.salattracker.domain.model.Mosque
import com.salattracker.domain.model.MosqueMode
import com.salattracker.domain.model.MosqueModeState
import com.salattracker.domain.model.NearbyMosque
import com.salattracker.domain.model.RingerMode
import com.salattracker.domain.usecase.MosqueModeController
import com.salattracker.testutil.FakeLocationProvider
import com.salattracker.testutil.FakeMosqueGeofencer
import com.salattracker.testutil.FakeMosqueModeNotifier
import com.salattracker.testutil.FakeMosqueModeStore
import com.salattracker.testutil.FakeMosqueRepository
import com.salattracker.testutil.FakeRingerController
import com.salattracker.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class MosqueModeViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val alNoor = Mosque(1, "Al-Noor", 24.0, 39.0, 100)
    private val repository = FakeMosqueRepository()
    private val store = FakeMosqueModeStore()
    private val geofencer = FakeMosqueGeofencer()
    private val ringer = FakeRingerController()
    private val notifier = FakeMosqueModeNotifier()
    private val locationProvider = FakeLocationProvider()
    private val controller = MosqueModeController(repository, store, geofencer, ringer, notifier)

    private fun createViewModel() = MosqueModeViewModel(repository, store, controller, locationProvider, ringer)

    @Test
    fun `state mirrors the saved mosques and the master switch`() {
        val viewModel = MosqueModeViewModel(
            FakeMosqueRepository(listOf(alNoor)),
            FakeMosqueModeStore(MosqueModeState(enabled = true)),
            controller, locationProvider, ringer,
        )

        val state = viewModel.uiState.value
        assertTrue(state.enabled)
        assertEquals(listOf(alNoor), state.mosques)
        assertNull(state.vibrateActiveAt)
    }

    @Test
    fun `the active mosque is shown only while the app has the phone on vibrate`() {
        store.setStateForTest(MosqueModeState(enabled = true, insideIds = setOf(1), restoreTo = RingerMode.NORMAL, activeMosqueName = "Al-Noor"))
        val active = createViewModel()
        assertEquals("Al-Noor", active.uiState.value.vibrateActiveAt)

        store.setStateForTest(MosqueModeState(enabled = true, insideIds = setOf(1), restoreTo = null, activeMosqueName = null))
        val idle = createViewModel()
        assertNull(idle.uiState.value.vibrateActiveAt)
    }

    @Test
    fun `add current location asks for a precise fix and opens the sheet with it`() {
        locationProvider.nextLocation = 24.5 to 39.5
        val viewModel = createViewModel()

        viewModel.onAddCurrentLocation()

        assertEquals(true, locationProvider.lastHighAccuracy)
        val draft = viewModel.uiState.value.draft
        assertNotNull(draft)
        assertEquals(24.5, draft!!.latitude, 0.0)
        assertEquals(39.5, draft.longitude, 0.0)
        assertNull(draft.id)
        assertEquals(MosqueMode.DEFAULT_RADIUS_METERS, draft.radiusMeters)
        assertFalse(viewModel.uiState.value.isLocating)
    }

    @Test
    fun `add current location reports when no fix could be obtained`() {
        locationProvider.nextLocation = null
        val viewModel = createViewModel()

        viewModel.onAddCurrentLocation()

        assertNull(viewModel.uiState.value.draft)
        assertEquals(MosqueMessage.LOCATION_UNAVAILABLE, viewModel.uiState.value.message)
    }

    @Test
    fun `saving with a blank name is rejected and keeps the sheet open`() {
        locationProvider.nextLocation = 24.5 to 39.5
        val viewModel = createViewModel()
        viewModel.onAddCurrentLocation()
        viewModel.onDraftNameChanged("   ")

        viewModel.onSaveMosque()

        assertTrue(viewModel.uiState.value.draft!!.nameMissing)
        assertTrue(repository.getMosquesBlocking().isEmpty())
    }

    @Test
    fun `saving a new mosque stores it trimmed with the chosen radius and closes the sheet`() {
        locationProvider.nextLocation = 24.5 to 39.5
        val viewModel = createViewModel()
        viewModel.onAddCurrentLocation()
        viewModel.onDraftNameChanged("  Masjid Quba ")
        viewModel.onDraftRadiusChanged(150)

        viewModel.onSaveMosque()

        val saved = repository.getMosquesBlocking().single()
        assertEquals("Masjid Quba", saved.name)
        assertEquals(150, saved.radiusMeters)
        assertEquals(24.5, saved.latitude, 0.0)
        assertNull(viewModel.uiState.value.draft)
    }

    @Test
    fun `saving while enabled re-registers the geofences`() {
        store.setStateForTest(MosqueModeState(enabled = true))
        locationProvider.nextLocation = 24.5 to 39.5
        val viewModel = createViewModel()
        viewModel.onAddCurrentLocation()
        viewModel.onDraftNameChanged("Masjid Quba")

        viewModel.onSaveMosque()

        assertEquals(1, geofencer.registered.last().size)
    }

    @Test
    fun `saving while disabled does not touch the geofences`() {
        locationProvider.nextLocation = 24.5 to 39.5
        val viewModel = createViewModel()
        viewModel.onAddCurrentLocation()
        viewModel.onDraftNameChanged("Masjid Quba")

        viewModel.onSaveMosque()

        assertTrue(geofencer.registered.isEmpty())
        assertEquals(0, geofencer.clearCount)
    }

    @Test
    fun `a failed save keeps the sheet and shows a message`() {
        repository.failWrites = true
        locationProvider.nextLocation = 24.5 to 39.5
        val viewModel = createViewModel()
        viewModel.onAddCurrentLocation()
        viewModel.onDraftNameChanged("Masjid Quba")

        viewModel.onSaveMosque()

        assertNotNull(viewModel.uiState.value.draft)
        assertEquals(MosqueMessage.SAVE_FAILED, viewModel.uiState.value.message)
    }

    @Test
    fun `editing an existing mosque updates it in place`() {
        val seeded = FakeMosqueRepository(listOf(alNoor))
        val viewModel = MosqueModeViewModel(seeded, store, controller, locationProvider, ringer)

        viewModel.onMosqueClicked(alNoor)
        viewModel.onDraftNameChanged("Al-Noor Mosque")
        viewModel.onDraftRadiusChanged(200)
        viewModel.onSaveMosque()

        val saved = seeded.getMosquesBlocking().single()
        assertEquals(1L, saved.id)
        assertEquals("Al-Noor Mosque", saved.name)
        assertEquals(200, saved.radiusMeters)
    }

    @Test
    fun `deleting needs a confirmation and then removes the mosque`() {
        val seeded = FakeMosqueRepository(listOf(alNoor))
        val viewModel = MosqueModeViewModel(seeded, store, controller, locationProvider, ringer)
        viewModel.onMosqueClicked(alNoor)

        viewModel.onDeleteClick()
        assertTrue(viewModel.uiState.value.showDeleteConfirm)
        viewModel.onDeleteDismiss()
        assertFalse(viewModel.uiState.value.showDeleteConfirm)
        assertEquals(1, seeded.getMosquesBlocking().size)

        viewModel.onDeleteClick()
        viewModel.onConfirmDelete()

        assertTrue(seeded.getMosquesBlocking().isEmpty())
        assertNull(viewModel.uiState.value.draft)
        assertFalse(viewModel.uiState.value.showDeleteConfirm)
    }

    @Test
    fun `adding is refused at the mosque limit`() {
        val full = FakeMosqueRepository((1L..MosqueMode.MAX_MOSQUES).map { Mosque(it, "M$it", 24.0, 39.0) })
        val viewModel = MosqueModeViewModel(full, store, controller, locationProvider, ringer)

        viewModel.onAddCurrentLocation()
        assertEquals(MosqueMessage.LIMIT_REACHED, viewModel.uiState.value.message)
        assertEquals(0, locationProvider.callCount)

        viewModel.onMessageDismiss()
        viewModel.onFindNearby()
        assertEquals(MosqueMessage.LIMIT_REACHED, viewModel.uiState.value.message)
        assertEquals(0, full.findNearbyCalls)
    }

    @Test
    fun `nearby search lists results and marks the ones already saved`() {
        val seeded = FakeMosqueRepository(listOf(alNoor))
        seeded.nearby = listOf(
            NearbyMosque("Al-Noor", 24.0, 39.0, 20),
            NearbyMosque("Jamia", 24.01, 39.0, 1100),
        )
        locationProvider.nextLocation = 24.0 to 39.0
        val viewModel = MosqueModeViewModel(seeded, store, controller, locationProvider, ringer)

        viewModel.onFindNearby()

        val nearby = viewModel.uiState.value.nearby!!
        assertFalse(nearby.isLoading)
        assertNull(nearby.failure)
        assertEquals(listOf(true, false), nearby.results.map { it.alreadyAdded })
    }

    @Test
    fun `nearby search with no results is empty, not failed`() {
        locationProvider.nextLocation = 24.0 to 39.0
        val viewModel = createViewModel()

        viewModel.onFindNearby()

        val nearby = viewModel.uiState.value.nearby!!
        assertFalse(nearby.isLoading)
        assertNull(nearby.failure)
        assertTrue(nearby.results.isEmpty())
    }

    @Test
    fun `nearby search failure can be retried`() {
        locationProvider.nextLocation = 24.0 to 39.0
        repository.nearbyError = IOException("offline")
        val viewModel = createViewModel()

        viewModel.onFindNearby()
        assertEquals(NearbyFailure.SEARCH_FAILED, viewModel.uiState.value.nearby!!.failure)

        repository.nearbyError = null
        repository.nearby = listOf(NearbyMosque("Jamia", 24.01, 39.0, 1100))
        viewModel.onNearbyRetry()

        val nearby = viewModel.uiState.value.nearby!!
        assertNull(nearby.failure)
        assertEquals(1, nearby.results.size)
    }

    @Test
    fun `nearby search without a location fix reports a location failure without calling the network`() {
        locationProvider.nextLocation = null
        val viewModel = createViewModel()

        viewModel.onFindNearby()

        assertEquals(NearbyFailure.LOCATION_UNAVAILABLE, viewModel.uiState.value.nearby!!.failure)
        assertEquals(0, repository.findNearbyCalls)
    }

    @Test
    fun `choosing a nearby result opens the sheet prefilled, and an added one is ignored`() {
        locationProvider.nextLocation = 24.0 to 39.0
        repository.nearby = listOf(NearbyMosque("Jamia", 24.01, 39.02, 1100))
        val viewModel = createViewModel()
        viewModel.onFindNearby()
        val item = viewModel.uiState.value.nearby!!.results.single()

        viewModel.onNearbySelected(item.copy(alreadyAdded = true))
        assertNull(viewModel.uiState.value.draft)

        viewModel.onNearbySelected(item)

        assertNull(viewModel.uiState.value.nearby)
        val draft = viewModel.uiState.value.draft!!
        assertEquals("Jamia", draft.name)
        assertEquals(24.01, draft.latitude, 0.0)
        assertEquals(39.02, draft.longitude, 0.0)
    }

    @Test
    fun `an unnamed nearby result opens the sheet with an empty name`() {
        locationProvider.nextLocation = 24.0 to 39.0
        repository.nearby = listOf(NearbyMosque(null, 24.01, 39.0, 1100))
        val viewModel = createViewModel()
        viewModel.onFindNearby()

        viewModel.onNearbySelected(viewModel.uiState.value.nearby!!.results.single())

        assertEquals("", viewModel.uiState.value.draft!!.name)
    }

    @Test
    fun `turning the switch on registers geofences and off clears them`() {
        repository.addForTest(alNoor)
        val viewModel = createViewModel()

        viewModel.onEnabledChanged(true)
        assertTrue(viewModel.uiState.value.enabled)
        assertEquals(listOf(alNoor), geofencer.registered.last())

        viewModel.onEnabledChanged(false)
        assertFalse(viewModel.uiState.value.enabled)
        assertEquals(1, geofencer.clearCount)
    }

    @Test
    fun `location being off is surfaced from the geofence result`() {
        repository.addForTest(alNoor)
        geofencer.result = GeofenceSync.LOCATION_OFF
        val viewModel = createViewModel()

        viewModel.onEnabledChanged(true)

        assertTrue(viewModel.uiState.value.locationOff)
    }

    @Test
    fun `a geofence failure shows a message`() {
        repository.addForTest(alNoor)
        geofencer.result = GeofenceSync.FAILED
        val viewModel = createViewModel()

        viewModel.onEnabledChanged(true)

        assertEquals(MosqueMessage.SYNC_FAILED, viewModel.uiState.value.message)
    }

    @Test
    fun `regaining full access while enabled re-registers once`() {
        store.setStateForTest(MosqueModeState(enabled = true))
        repository.addForTest(alNoor)
        val viewModel = createViewModel()

        viewModel.onAccessChanged(LocationAccess.FOREGROUND_ONLY)
        assertTrue(geofencer.registered.isEmpty())

        viewModel.onAccessChanged(LocationAccess.FULL)
        assertEquals(1, geofencer.registered.size)

        viewModel.onAccessChanged(LocationAccess.FULL)
        assertEquals(1, geofencer.registered.size)
    }

    @Test
    fun `restore sound puts the ringer back`() {
        store.setStateForTest(MosqueModeState(enabled = true, insideIds = setOf(1), restoreTo = RingerMode.NORMAL, activeMosqueName = "Al-Noor"))
        repository.addForTest(alNoor)
        ringer.mode = RingerMode.VIBRATE
        val viewModel = createViewModel()

        viewModel.onRestoreSound()

        assertEquals(RingerMode.NORMAL, ringer.mode)
        assertNull(viewModel.uiState.value.vibrateActiveAt)
    }

    @Test
    fun `the state reports a device that cannot vibrate`() {
        ringer.canVibrate = false

        assertFalse(createViewModel().uiState.value.canVibrate)
    }
}
