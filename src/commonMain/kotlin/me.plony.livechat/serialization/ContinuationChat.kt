package me.plony.livechat.serialization

import kotlinx.serialization.Serializable

@Serializable
data class ContinuationChat(
    val timedContinuationData: TimedContinuationData? = null,
    val invalidationContinuationData: TimedContinuationData? = null
) {
    @Serializable
    data class TimedContinuationData(
        val continuation: String,
        val timeoutMs: Long
    )
}