package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.listener
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class DoNotPassThisDecree : Decree(
    "Do Not Pass This Decree",
    "\uD83D\uDE45",
    "Oh my god please don't everyone will hate you",
    true
) {
    override suspend fun execute(init: Boolean) {
        Bot.jda.listener<MessageReceivedEvent> { event ->
            if (!isApplicableTo(event.channel, event.author)) return@listener

            val content = event.message.contentRaw
            if (content.isEmpty()) return@listener
            if (content.length == 2000) return@listener

            event.message.delete().await()
        }
    }
}