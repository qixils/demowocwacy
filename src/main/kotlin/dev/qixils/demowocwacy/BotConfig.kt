package dev.qixils.demowocwacy

import kotlinx.serialization.Serializable

@Serializable
data class BotConfig(
    val token: String,
    val guild: Long,
    val channel: Long, // channel to send election messages in
    val protectedChannels: List<Long>,
    val protectedUsers: List<Long>,
    val unseriousChannels: List<Long>
)
