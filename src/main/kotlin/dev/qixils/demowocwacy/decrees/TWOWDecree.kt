package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.events.listener
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class TWOWDecree : Decree(
    "Ten Words of Wisdom",
    "\uD83D\uDCD8",
    "Users will no longer be able to send messages larger than ten words",
    persistent = true,
) {
    override suspend fun execute() {
        // TODO: maybe automod rule?
        Bot.jda.listener<MessageReceivedEvent> {
            if (!isApplicableTo(it.channel, it.author)) return@listener
            if (it.message.contentRaw.split(" ").size > 10) {
                it.message.delete().queue()
            }
        }
    }
}