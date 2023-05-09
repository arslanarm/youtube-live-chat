package me.plony.livechat.timeout

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration

abstract class TimeoutStrategy {
    var nextTime: Instant? = null
    fun setNewTimeout(timeout: Duration) {
        nextTime = Clock.System.now() + timeout
    }
    abstract suspend fun wait()
}