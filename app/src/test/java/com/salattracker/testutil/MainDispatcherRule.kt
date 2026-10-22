package com.salattracker.testutil

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Routes [Dispatchers.Main] (what `viewModelScope` launches on) to a test dispatcher, so a
 * ViewModel's `init`-block coroutines run synchronously within the test instead of hanging.
 * [scheduler] is exposed so a test covering a `delay()`-based debounce (e.g. a search field) can
 * advance virtual time explicitly (`scheduler.advanceTimeBy(...); scheduler.runCurrent()`) —
 * everything else can ignore it. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val scheduler: TestCoroutineScheduler = TestCoroutineScheduler(),
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher(scheduler),
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}
