package dev.qixils.demowocwacy

import dev.qixils.demowocwacy.decrees.UnseriousConfig
import kotlinx.serialization.Serializable

@Serializable
data class BotConfig(
    val token: String,
    val guild: Long,
    val channel: Long, // channel to send election messages in
    val protectedChannels: List<Long>,
    val protectedUsers: List<Long>,
    val decrees: DecreeConfig,
)

@Serializable
data class DecreeConfig(
    val unserious: UnseriousConfig,
)
