package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.events.listener
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.util.concurrent.TimeUnit

class ASMRDecree : Decree(
    "ASMR",
    "\uD83C\uDF99\uFE0F",
    "Only allow speaking through voice messages",
    true
) {
    override suspend fun execute(init: Boolean) {
        Bot.jda.listener<MessageReceivedEvent> { event ->
            if (!isApplicableTo(event.message)) return@listener
            if (event.message.type.isSystem) return@listener

            if (Message.MessageFlag.IS_VOICE_MESSAGE in event.message.flags) return@listener

            event.message.delete().reason(event.message.id).queueAfter(500, TimeUnit.MILLISECONDS)
        }
    }
}