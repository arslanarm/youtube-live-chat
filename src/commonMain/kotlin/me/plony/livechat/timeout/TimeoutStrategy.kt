package me.plony.livechat.timeout

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration

abstract class TimeoutStrategy {
    var invalidationTimeout: Instant? = null
    var timedTimeout: Instant? = null

    fun setInvalidationTimeout(timeout: Duration) {
        invalidationTimeout = Clock.System.now() + timeout
        timedTimeout = null
    }
    fun setTimedTimeout(timeout: Duration) {
        timedTimeout = Clock.System.now() + timeout
        invalidationTimeout = null
    }
    abstract suspend fun wait()

    protected fun anyTimeout() = timedTimeout != null || invalidationTimeout != null
}