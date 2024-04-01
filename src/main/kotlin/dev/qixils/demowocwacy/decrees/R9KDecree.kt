package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.listener
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class R9KDecree : Decree(
    "Robot9000",
    "\uD83D\uDC6E",
    "Ensures every sent message is unique",
    true
) {
    private val messages = mutableSetOf<String>() // TODO: state? probably not

    override suspend fun execute(init: Boolean) {
        Bot.jda.listener<MessageReceivedEvent> { event ->
            if (!isApplicableTo(event.channel, event.author)) return@listener

            val content = event.message.contentRaw
            if (content.isEmpty()) return@listener
            if (messages.add(content)) return@listener

            event.message.delete().await()
        }
    }
}