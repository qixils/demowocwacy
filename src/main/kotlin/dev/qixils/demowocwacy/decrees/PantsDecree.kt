package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.coroutines.await
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import kotlinx.serialization.Serializable

class PantsDecree : Decree(
    "Pants",
    "\uD83D\uDC56",
    "Turns every discussion channel into #pants",
    false
) {
    private val categories = Bot.config.decrees.pants.categories.mapNotNull { Bot.guild.getCategoryById(it) }

    override suspend fun execute(init: Boolean) {
        for (category in categories) {
            for (channel in category.channels) {
                if (channel.idLong in Bot.config.protectedChannels) continue
                Bot.state.decrees.pants.names[channel.idLong] = channel.name
                channel.manager.setName("pants").await()
            }
        }
    }

    override suspend fun cleanup() {
        for ((channelId, name) in Bot.state.decrees.pants.names) {
            val channel = Bot.guild.getGuildChannelById(channelId) ?: continue
            if (channel.name != "pants") continue
            if (channel.name == name) continue
            channel.manager.setName(name).await()
        }
    }
}

@Serializable
data class PantsConfig(
    val categories: List<Long> = listOf(0L),
)

@Serializable
data class PantsState(
    val names: MutableMap<Long, String> = mutableMapOf(),
)