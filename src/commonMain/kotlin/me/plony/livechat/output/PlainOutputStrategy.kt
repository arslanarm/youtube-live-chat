package me.plony.livechat.output

import me.plony.livechat.ChatMessage

class PlainOutputStrategy : OutputStrategy<List<ChatMessage>?>() {
    override suspend fun generateOutput(): List<ChatMessage>? = chat.fetchNextMessages()
}