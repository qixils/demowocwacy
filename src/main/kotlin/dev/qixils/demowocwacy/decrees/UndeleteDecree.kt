package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.send
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import dev.qixils.demowocwacy.decrees.base.WebhookDecree
import kotlinx.coroutines.delay
import net.dv8tion.jda.api.audit.ActionType
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

    override suspend fun execute(init: Boolean) {
        Bot.jda.listener<MessageReceivedEvent> { event ->
            if (!isApplicableTo(event.message)) return@listener
            if (event.message.type.isSystem) return@listener
            messages[event.messageIdLong] = event.message
        }
        Bot.jda.listener<MessageUpdateEvent> { event ->
            if (event.messageIdLong !in messages) return@listener
            messages[event.messageIdLong] = event.message
        }
        Bot.jda.listener<MessageDeleteEvent> { event ->
            val message = messages.remove(event.messageIdLong) ?: return@listener
            if (event.messageIdLong in WebhookDecree.processed) return@listener
            delay(500)
            if (Bot.guild.retrieveAuditLogs().type(ActionType.MESSAGE_DELETE).limit(5).await().any { it.reason == event.messageId }) return@listener // attempt to prevent self logging
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
            })).await()
        }
    }
}