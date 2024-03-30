package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.listener
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import net.dv8tion.jda.api.entities.Webhook
import net.dv8tion.jda.api.entities.channel.attribute.IWebhookContainer
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class ReverseDecree : Decree(
    "esreveR",
    "\uD83D\uDD01",
    "Reverses all sent messages",
    true
) {
    private val webhookName = "demowocracy"
    private val webhooks = mutableMapOf<Long, Webhook>()

    override suspend fun execute() {
        Bot.jda.listener<MessageReceivedEvent> { event ->
            val member = event.member ?: return@listener
            val channel = event.channel
            if (!isApplicableTo(channel)) return@listener
            if (event.message.type.isSystem) return@listener
            if (channel !is IWebhookContainer) return@listener

            if (event.channel.idLong !in webhooks) {
                webhooks[event.channel.idLong] =
                    channel.retrieveWebhooks().await().find { wh -> wh.name == webhookName }
                    ?: channel.createWebhook(webhookName).await()
            }
            val webhook = webhooks[event.channel.idLong]!!

            webhook.sendMessage(event.message.contentRaw.reversed())
                .setUsername(member.effectiveName.reversed())
                .setAvatarUrl(member.effectiveAvatarUrl)
                .await()
        }
    }
}