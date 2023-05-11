package me.plony.livechat.timeout

import kotlinx.coroutines.delay
import kotlin.time.Duration

class ConstantTimeoutStrategy(val timeout: Duration) : TimeoutStrategy() {
    override suspend fun wait() = delay(timeout)
}