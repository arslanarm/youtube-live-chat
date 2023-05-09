package me.plony.livechat.serialization

import kotlinx.serialization.Serializable

@Serializable
data class Action(
    val addChatItemAction: AddChatItemAction? = null
) {
    @Serializable
    data class AddChatItemAction(
        val item: Item
    ) {
        @Serializable
        data class Item(
            val liveChatTextMessageRenderer: LiveChatTextMessageRenderer? = null
        ) {
            @Serializable
            data class LiveChatTextMessageRenderer(
                val message: Message,
                val authorName: AuthorName,
                val timestampUsec: String
            ) {
                @Serializable
                data class Message(
                    val runs: List<Runs>,
                )
                @Serializable
                data class AuthorName(
                    val simpleText: String
                )
            }
        }
    }
}

@Serializable
data class Runs(
    val text: String = "",
    val emoji: Emoji? = null
) {
    @Serializable
    data class Emoji(
        val emojiId: String,
        val isCustomEmoji: Boolean = false,
        val image: Image
    ) {
        @Serializable
        data class Image(
            val thumbnails: List<Thumbnail>
        ) {
            @Serializable
            data class Thumbnail(
                val url: String
            )
        }
    }
}