package dev.qixils.demowocwacy

import kotlinx.serialization.Serializable

@Serializable
data class BotConfig(
    val token: String,
    val guild: Long,
    val protectedChannels: List<Long>,
    val protectedUsers: List<Long>,
)
