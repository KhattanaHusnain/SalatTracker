package com.salattracker.core.util

import kotlinx.coroutines.CancellationException

/**
 * Like [kotlin.runCatching], except a [CancellationException] is rethrown instead of being
 * wrapped as a failure.
 *
 * Plain `runCatching` catches every [Throwable], cancellation included — so a suspend call inside
 * it that gets cancelled (e.g. a per-section `Job` replaced by a faster period/tab switch, or the
 * ViewModel itself being cleared) reports itself as a failed load and surfaces a spurious
 * "StandaloneCoroutine was cancelled" error to the user instead of just quietly stopping.
 */
inline fun <T> runCatchingCancellable(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    Result.failure(e)
}
