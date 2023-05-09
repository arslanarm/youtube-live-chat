package me.plony.livechat.serialization

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class YoutubeConfiguration(
    @SerialName("INNERTUBE_API_KEY")
    val innertubeApiKey: String,
    @SerialName("INNERTUBE_CONTEXT")
    val innertubeContext: InnertubeContext,
) {
    @Serializable
    data class InnertubeContext(
        val client: Client
    ) {
        @Serializable
        data class Client(
            val hl: String = "",
            val gl: String = "",
            val remoteHost: String = "",
            val deviceMake: String = "",
            val deviceModel: String = "",
            val visitorData: String = "",
            val userAgent: String = "",
            val clientName: String = "",
            val clientVersion: String = "",
            val osName: String = "",
            val osVersion: String = "",
            val originalUrl: String = "",
            val platform: String = "",
            val clientFormFactor: String = "",
            val configInfo: ConfigInfo = ConfigInfo()
        ){
            @Serializable
            data class ConfigInfo(
                val appInstallData: String = ""
            )
        }
    }
}