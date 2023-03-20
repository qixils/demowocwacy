package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.send
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.MessageUpdateEvent
import net.dv8tion.jda.api.utils.TimeUtil
import java.time.OffsetDateTime
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class UndeleteDecree : Decree(
    "Undelete Messages",
    "\uD83D\uDEAF",
    "Publicly posts every deleted message",
    true
) {
    private val messages = mutableMapOf<Long, Message>()
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private val maxAge = 10L // minutes

    init {
        executor.scheduleAtFixedRate({
            val cutoff = OffsetDateTime.now().minusMinutes(maxAge)
            messages.entries.removeIf { TimeUtil.getTimeCreated(it.key).isBefore(cutoff) }
        }, maxAge, 1, TimeUnit.MINUTES)
    }

    override suspend fun execute() {
        Bot.jda.listener<MessageReceivedEvent> {
            if (!isApplicableTo(it.channel)) return@listener
            if (it.message.type.isSystem) return@listener
            messages[it.messageIdLong] = it.message
        }
        Bot.jda.listener<MessageUpdateEvent> {
            if (it.messageIdLong !in messages) return@listener
            messages[it.messageIdLong] = it.message
        }
        Bot.jda.listener<MessageDeleteEvent> {
            if (it.messageIdLong !in messages) return@listener
            val message = messages[it.messageIdLong]!!
            message.channel.send(embeds = listOf(Embed {
                author {
                    name = message.member!!.effectiveName
                    iconUrl = message.author.effectiveAvatarUrl
                }
                title = "Deleted Message"
                description = message.contentRaw
                timestamp = TimeUtil.getTimeCreated(message.idLong)
                footer {
                    name = "Services provided by Messages & Decrees Administration"
                }
            }))
        }
    }
}