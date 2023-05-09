package me.plony.livechat.serialization

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessages(
    val continuationContents: ContinuationContents
) {
    @Serializable
    data class ContinuationContents(
        val liveChatContinuation: LiveChatContinuation
    ) {
        @Serializable
        data class LiveChatContinuation(
            val actions: List<Action>,
            val continuations: List<ContinuationChat>
        )
    }
}