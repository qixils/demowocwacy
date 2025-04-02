package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.events.listener
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.util.concurrent.TimeUnit

class PalindromeDecree : Decree(
    "PalindromesemordnilaP",
    "➰",
    "Block messages that aren't palindromes",
    true
) {
    override suspend fun execute(init: Boolean) {
        Bot.jda.listener<MessageReceivedEvent> { event ->
            if (!isApplicableTo(event.message)) return@listener
            if (event.message.type.isSystem) return@listener

            val content = event.message.contentRaw.lowercase()
            if (content == content.reversed()) return@listener

            event.message.delete().reason(event.message.id).queueAfter(500, TimeUnit.MILLISECONDS)
        }
    }
}