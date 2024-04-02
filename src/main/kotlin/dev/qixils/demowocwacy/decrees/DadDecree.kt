package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.messages.reply_
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class DadDecree : Decree(
    "Dad",
    "\uD83D\uDC68",
    "Hi USER, I'm Dad.",
    true
) {
    private val pattern = Regex("I(?:'| +a)m +(.+)", setOf(RegexOption.IGNORE_CASE))

    override suspend fun execute(init: Boolean) {
        Bot.jda.listener<MessageReceivedEvent> { event ->
            if (!isApplicableTo(event.message)) return@listener
            if (event.message.type.isSystem) return@listener

            val match = pattern.find(event.message.contentRaw) ?: return@listener
            event.message.reply_("Hi ${match.groupValues[1]}, I'm ${Bot.jda.selfUser.effectiveName}.").await()
        }
    }
}