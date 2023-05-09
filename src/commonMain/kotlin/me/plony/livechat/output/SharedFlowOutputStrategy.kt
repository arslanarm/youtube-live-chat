package me.plony.livechat.output

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import me.plony.livechat.ChatMessage
import me.plony.livechat.LiveChat

class SharedFlowOutputStrategy(val scope: CoroutineScope) : OutputStrategy<SharedFlow<ChatMessage>>() {
    lateinit var messages: SharedFlow<ChatMessage>
    private val back = ColdFlowOutputStrategy()
    override fun init(chat: LiveChat<*>) {
        super.init(chat)
        back.init(chat)
    }

    override suspend fun generateOutput(): SharedFlow<ChatMessage> {
        if (::messages.isInitialized) return messages
        messages = back.generateOutput().shareIn(scope, SharingStarted.Eagerly)
        return messages
    }
}