package dev.qixils.demowocwacy.decrees.base

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.CoroutineEventListener
import dev.minn.jda.ktx.events.listener
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Webhook
import net.dv8tion.jda.api.entities.channel.attribute.IWebhookContainer
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.util.concurrent.TimeUnit

abstract class WebhookDecree : Decree {

    constructor(name: String, emoji: String, description: String) : super(name, emoji, description, true)
    constructor(name: String, emoji: Emoji, description: String) : super(name, emoji, description, true)

    companion object {
        val processed = mutableSetOf<Long>()
        private const val webhookName = "demowocracy"
        private val webhooks = mutableMapOf<Long, Webhook>()
        private var listener: CoroutineEventListener? = null
        private val filters = mutableListOf<WebhookDecree>()

        suspend fun getWebhook(channel: IWebhookContainer): Webhook {
            if (channel.idLong !in webhooks) {
                webhooks[channel.idLong] =
                    channel.retrieveWebhooks().await().find { wh -> wh.name == webhookName }
                        ?: channel.createWebhook(webhookName).await()
            }
            return webhooks[channel.idLong]!!
        }

        suspend fun sendAs(to: Webhook, from: Member, content: String) {
            to.sendMessage(content)
                .setUsername(from.effectiveName)
                .setAvatarUrl(from.effectiveAvatarUrl)
                .await()
        }

        fun applyFilters(text: String): String {
            var filtered = text
            for (filter in filters)
                filtered = filter.alter(filtered) ?: filtered
            return filtered
        }

        fun addFilter(decree: WebhookDecree) {
            filters.add(decree)
            if (listener == null) {
                listener = Bot.jda.listener<MessageReceivedEvent> { event ->
                    val channel = event.channel
                    if (!isApplicableTo(event.message)) return@listener
                    if (event.message.type.isSystem) return@listener
                    if (event.message.contentRaw.isEmpty()) return@listener
                    if (channel !is IWebhookContainer) return@listener

                    val webhook = getWebhook(channel)

                    val content = event.message.contentRaw
                    val filtered = applyFilters(content)

                    if (filtered == content) return@listener

                    processed.add(event.message.idLong)
                    event.message.delete().reason(event.message.id).queueAfter(500, TimeUnit.MILLISECONDS)

                    if (filtered.isEmpty()) return@listener

                    sendAs(webhook, event.member!!, filtered)
                }
            }
        }
    }

    override suspend fun execute(init: Boolean) {
        addFilter(this)
    }

    abstract fun alter(content: String): String?
}