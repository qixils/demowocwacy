package dev.qixils.demowocwacy.decrees.base

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.listener
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import net.dv8tion.jda.api.entities.Webhook
import net.dv8tion.jda.api.entities.channel.attribute.IWebhookContainer
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

abstract class WebhookDecree(name: String, emoji: String, description: String) : Decree(name, emoji, description, true) {
    private val webhookName = "demowocracy"
    private val webhooks = mutableMapOf<Long, Webhook>()

    // TODO: only register one listener

    override suspend fun execute() {
        Bot.jda.listener<MessageReceivedEvent> { event ->
            val member = event.member ?: return@listener
            val channel = event.channel
            if (!isApplicableTo(channel, event.author)) return@listener
            if (event.message.type.isSystem) return@listener
            if (event.message.contentRaw.isEmpty()) return@listener
            if (channel !is IWebhookContainer) return@listener

            if (event.channel.idLong !in webhooks) {
                webhooks[event.channel.idLong] =
                    channel.retrieveWebhooks().await().find { wh -> wh.name == webhookName }
                        ?: channel.createWebhook(webhookName).await()
            }
            val webhook = webhooks[event.channel.idLong]!!

            webhook.sendMessage(alter(event.message.contentRaw) ?: return@listener)
                .setUsername(member.effectiveName)
                .setAvatarUrl(member.effectiveAvatarUrl)
                .await()
        }
    }

    abstract fun alter(content: String): String?
}