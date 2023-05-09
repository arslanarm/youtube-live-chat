package me.plony.livechat.output

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import me.plony.livechat.ChatMessage
import me.plony.livechat.exceptions.StreamIsClosed

class ColdFlowOutputStrategy : OutputStrategy<Flow<ChatMessage>>() {
    override suspend fun generateOutput(): Flow<ChatMessage> = flow {
        while (true) {
            val messages = try {
                chat.fetchNextMessages() ?: continue
            } catch (e: StreamIsClosed) {
                break
            }
            emitAll(messages.asFlow())
        }
    }
}