package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.coroutines.await
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.entities.channel.concrete.Category
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ChaChaSlideDecree : Decree(
    "Cha-Cha Slide",
    "\uD83D\uDD00",
    "The channel order is constantly moved around",
    true
) {
    private val targets: List<Category>
        get() = Bot.config.decrees.chaChaSlide.targets.mapNotNull { Bot.guild.getCategoryById(it) }
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private val random = Random()
    private val range = 1..10 // seconds

    override suspend fun execute(init: Boolean) {
        if (init) {
            for (target in targets) {
                for (channel in target.textChannels) {
                    Bot.state.decrees.chaChaSlide.order[channel.idLong] = channel.positionInCategory
                }
            }
            Bot.saveState()
        }
        schedule()
    }

    private fun schedule() {
        executor.schedule(
            this::shuffle,
            random.nextLong(range.first * 1_000L, range.last * 1_000L),
            TimeUnit.MILLISECONDS
        )
    }

    private fun shuffle() = runBlocking {
        try {
            for (target in targets) {
                Bot.logger.debug("Shuffling $target")
                val action = target.modifyTextChannelPositions()
                action.shuffleOrder()
                for (channel in action.currentOrder.toList()) {
                    // ensure inapplicable channels maintain their order
                    if (channel.idLong !in Bot.config.protectedChannels) continue
                    val destination = Bot.state.decrees.chaChaSlide.order[channel.idLong] ?: continue
                    action.selectPosition(channel).moveTo(destination)
                }
                action.await()
            }
        } catch (e: Exception) {
            Bot.logger.error("Failed to shuffle", e)
        }
    }
}

@Serializable
data class ChaChaSlideState(
    val order: MutableMap<Long, Int> = mutableMapOf(), // the original channel order
)

@Serializable
data class ChaChaSlideConfig(
    val targets: List<Long> = listOf(), // category IDs
)