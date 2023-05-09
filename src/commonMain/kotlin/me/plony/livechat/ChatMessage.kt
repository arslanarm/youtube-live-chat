package me.plony.livechat

import kotlinx.datetime.Instant

data class ChatMessage(
    val authorName: String,
    val message: String,
    val timestamp: Instant
)