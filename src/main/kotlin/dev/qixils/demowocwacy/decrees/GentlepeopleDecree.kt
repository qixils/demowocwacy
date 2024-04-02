package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.events.listener
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.util.concurrent.TimeUnit

class GentlepeopleDecree : Decree(
    "Proper Gentlepeople",
    "\uD83E\uDDD1\u200D\uD83D\uDCBC",
    "Speak only like a proper, well-mannered individual.",
    true
) {
    private val punctuation = ".!?‼‽¡¿"

    override suspend fun execute(init: Boolean) {
        Bot.jda.listener<MessageReceivedEvent> { event ->
            if (!isApplicableTo(event.message)) return@listener

            val content = event.message.contentRaw
            if (content.isEmpty()) return@listener
            if ((content[0].isUpperCase() || content[0] in punctuation) && content.last() in punctuation) return@listener

            event.message.delete().reason(event.message.id).queueAfter(500, TimeUnit.MILLISECONDS)
        }
    }
}