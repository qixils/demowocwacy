package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.listener
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.utils.TimeUtil
import java.time.Instant
import java.util.concurrent.TimeUnit

class JanitorDecree : Decree(
    "Janitor",
    "\uD83D\uDDD1\uFE0F",
    "Allow deleting any message by popular vote",
    true
) {
    private var start: Instant
        get() = Instant.parse(Bot.state.decrees.janitor.start)
        set(value) {
            Bot.state.decrees.janitor.start = value.toString()
        }
    private val react = Emoji.fromUnicode("\uD83D\uDDD1\uFE0F")
    private val amount = 10

    override suspend fun execute(init: Boolean) {
        if (init) {
            start = Instant.now()
        }
        Bot.jda.listener<MessageReactionAddEvent> { event ->
            if (!isApplicableTo(event.channel)) return@listener
            if (event.emoji != react) return@listener
            if (TimeUtil.getTimeCreated(event.messageIdLong).toInstant().isBefore(start)) return@listener

            val message = event.retrieveMessage().await()

            val count = message.getReaction(event.emoji)?.count ?: return@listener
            if (count < amount) return@listener

            message.delete().reason(message.id).queueAfter(500, TimeUnit.MILLISECONDS)
        }
    }
}

@Serializable
data class JanitorState(
    var start: String = Instant.MIN.toString(),
)
