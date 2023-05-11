package me.plony.livechat.timeout

import kotlinx.coroutines.delay
import kotlinx.datetime.Clock

class DelayTimeoutStrategy : TimeoutStrategy() {

    override suspend fun wait() {
        (invalidationTimeout ?: timedTimeout)?.let {
            delay(Clock.System.now() - it)
        }
    }
}