package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.listener
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class SpeechlessDecree : Decree(
    "A Thousand Words",
    "\uD83D\uDDBC\uFE0F",
    "Remove the need for ordinary speech",
    true
) {
    override suspend fun execute() {
        Bot.jda.listener<MessageReceivedEvent> { event ->
            if (!isApplicableTo(event.channel, event.author)) return@listener
            if (event.message.type.isSystem) return@listener

            if (event.message.contentRaw.isEmpty()) return@listener
            event.message.delete().await()
        }
    }
}