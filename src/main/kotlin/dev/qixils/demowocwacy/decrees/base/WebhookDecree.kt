package dev.qixils.demowocwacy.decrees.base

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.CoroutineEventListener
import dev.minn.jda.ktx.events.listener
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import net.dv8tion.jda.api.entities.Webhook
import net.dv8tion.jda.api.entities.channel.attribute.IWebhookContainer
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

abstract class WebhookDecree(name: String, emoji: String, description: String) : Decree(name, emoji, description, true) {

    companion object {
        private const val webhookName = "demowocracy"
        private val webhooks = mutableMapOf<Long, Webhook>()
        private var listener: CoroutineEventListener? = null
        private val filters = mutableListOf<WebhookDecree>()

        fun addFilter(decree: WebhookDecree) {
            filters.add(decree)
            if (listener == null) {
                listener = Bot.jda.listener<MessageReceivedEvent> { event ->
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

                    val content = event.message.contentRaw
                    var filtered = content
                    for (filter in filters)
                        filtered = filter.alter(filtered) ?: filtered

                    if (filtered == content) return@listener

                    event.message.delete().await()

                    if (filtered.isEmpty()) return@listener

                    webhook.sendMessage(filtered)
                        .setUsername(member.effectiveName)
                        .setAvatarUrl(member.effectiveAvatarUrl)
                        .await()
                }
            }
        }
    }

    override suspend fun execute() {
        addFilter(this)
    }

    abstract fun alter(content: String): String?
}