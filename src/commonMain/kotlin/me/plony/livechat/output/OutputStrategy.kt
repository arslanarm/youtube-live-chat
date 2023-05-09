package me.plony.livechat.output

import me.plony.livechat.ChatMessage
import me.plony.livechat.LiveChat

abstract class OutputStrategy<T> {
    lateinit var chat: LiveChat<*>
    open fun init(chat: LiveChat<*>) {
        this.chat = chat
    }
    abstract suspend fun generateOutput(): T
}