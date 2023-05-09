package me.plony.livechat.timeout


class NoTimeoutStrategy: TimeoutStrategy() {
    override suspend fun wait() {
    }
}