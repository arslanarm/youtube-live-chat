package me.plony.livechat.timeout

import kotlinx.coroutines.delay
import kotlinx.datetime.Clock

class DelayTimeoutStrategy : TimeoutStrategy() {

    override suspend fun wait() {
        nextTime?.let {
            delay(Clock.System.now() - it)
        }
    }
}