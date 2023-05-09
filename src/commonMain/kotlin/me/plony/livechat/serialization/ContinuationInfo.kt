package me.plony.livechat.serialization

import kotlinx.serialization.Serializable

@Serializable
data class ContinuationInfo(
    val reloadContinuationData: ReloadContinuationData
) {
    @Serializable
    data class ReloadContinuationData(
        val clickTrackingParams: String,
        val continuation: String
    )
}