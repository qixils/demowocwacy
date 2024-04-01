package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.listener
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.utils.TimeUtil
import java.time.OffsetDateTime
import java.time.ZoneOffset

class JanitorDecree : Decree(
    "Janitor",
    "\uD83D\uDDD1\uFE0F",
    "Allow deleting any message by popular vote",
    true
) {
    private val start = OffsetDateTime.of(2024, 4, 1, 0, 0, 0, 0, ZoneOffset.UTC)
    private val react = Emoji.fromUnicode("\uD83D\uDDD1\uFE0F")
    private val amount = 10

    override suspend fun execute(init: Boolean) {
        Bot.jda.listener<MessageReactionAddEvent> { event ->
            if (!isApplicableTo(event.channel)) return@listener
            if (event.emoji != react) return@listener
            if (TimeUtil.getTimeCreated(event.messageIdLong).isBefore(start)) return@listener

            val message = event.retrieveMessage().await()

            val count = message.getReaction(event.emoji)?.count ?: return@listener
            if (count < amount) return@listener

            message.delete().await()
        }
    }
}