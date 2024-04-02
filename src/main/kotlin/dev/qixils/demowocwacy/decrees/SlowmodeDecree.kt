package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.coroutines.await
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import kotlinx.serialization.Serializable

class SlowmodeDecree : Decree(
    "January 15, 1919 at approximately 12:30 PM",
    "\uD83D\uDD67",
    "S..l..o..w.. d..o..w..n..",
    false
) {
    private val categories get() = Bot.config.decrees.slowmode.categories.mapNotNull { Bot.guild.getCategoryById(it) }
    private val duration = 60 // seconds

    override suspend fun execute(init: Boolean) {
        for (category in categories) {
            for (channel in category.textChannels) {
                if (channel.idLong in Bot.config.protectedChannels) continue
                channel.manager.setSlowmode(duration).setDefaultThreadSlowmode(duration).await()
            }
        }
    }

    override suspend fun cleanup() {
        for (category in categories) {
            for (channel in category.textChannels) {
                if (channel.idLong in Bot.config.protectedChannels) continue
                channel.manager.setSlowmode(0).setDefaultThreadSlowmode(0).await()
            }
        }
    }
}

@Serializable
data class SlowmodeConfig(
    val categories: List<Long> = listOf(),
)
