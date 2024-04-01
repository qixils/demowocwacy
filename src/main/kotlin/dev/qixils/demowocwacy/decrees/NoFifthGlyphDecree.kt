package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.listener
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class NoFifthGlyphDecree : Decree(
    "HTwins STM+",
    "\uD83C\uDDEA",
    "Post without using our fifth glyph",
    true
) {
    override suspend fun execute(init: Boolean) {
        Bot.jda.listener<MessageReceivedEvent> { event ->
            if (!isApplicableTo(event.channel, event.author)) return@listener

            if (!event.member!!.effectiveName.contains('e') && !event.message.contentRaw.contains('e')) return@listener

            event.message.delete().await()
        }
    }
}