package com.salattracker.core.util

import com.salattracker.domain.model.CityResult
import com.salattracker.domain.repository.GeocodingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Shared cancellation and debounce policy for city autocomplete. */
class DebouncedCitySearch(
    private val scope: CoroutineScope,
    private val repository: GeocodingRepository,
    private val onSearching: (Boolean) -> Unit,
    private val onResults: (List<CityResult>) -> Unit,
) {
    private var job: Job? = null

    fun search(query: String) {
        job?.cancel()
        if (query.isBlank()) {
            onResults(emptyList())
            onSearching(false)
            return
        }
        job = scope.launch {
            onSearching(true)
            delay(DEBOUNCE_MILLIS)
            onResults(runCatching { repository.searchCities(query) }.getOrDefault(emptyList()))
            onSearching(false)
        }
    }

    companion object { const val DEBOUNCE_MILLIS = 350L }
}
