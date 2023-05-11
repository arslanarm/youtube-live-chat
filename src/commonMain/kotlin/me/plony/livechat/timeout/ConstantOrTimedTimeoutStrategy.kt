package me.plony.livechat.timeout

import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlin.time.Duration

class ConstantOrTimedTimeoutStrategy(
    val constantTimeoutDuration: Duration
) : TimeoutStrategy() {
    override suspend fun wait() {
        if (!anyTimeout()) return
        timedTimeout?.let {
            delay(it - Clock.System.now())
            println("TIMED OUT ${it - Clock.System.now()}")
        } ?: delay(constantTimeoutDuration)
    }
}