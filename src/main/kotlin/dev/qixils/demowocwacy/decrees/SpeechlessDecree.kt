package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.events.listener
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.util.concurrent.TimeUnit

class SpeechlessDecree : Decree(
    "A Thousand Words",
    "\uD83D\uDDBC\uFE0F",
    "Remove the need for ordinary speech",
    true
) {
    override suspend fun execute(init: Boolean) {
        Bot.jda.listener<MessageReceivedEvent> { event ->
            if (!isApplicableTo(event.message)) return@listener
            if (event.message.type.isSystem) return@listener

            if (event.message.contentRaw.isEmpty()) return@listener

            event.message.delete().queueAfter(500, TimeUnit.MILLISECONDS)
        }
    }
}