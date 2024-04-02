package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.events.listener
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import dev.qixils.demowocwacy.decrees.base.WebhookDecree
import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.entities.channel.attribute.IWebhookContainer
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class CloneDecree : Decree(
    "Clone",
    "\uD83D\uDC65",
    "Clone yourself",
    true
) {
    override suspend fun execute(init: Boolean) {
        if (init) {
            Bot.state.decrees.clone.user = Bot.state.election.primeMinister
            Bot.saveState()
        }

        Bot.jda.listener<MessageReceivedEvent> { event ->
            val channel = event.channel
            if (!isApplicableTo(event.message)) return@listener
            if (event.message.type.isSystem) return@listener
            if (event.message.contentRaw.isEmpty()) return@listener
            if (channel !is IWebhookContainer) return@listener

            if (event.author.idLong != Bot.state.decrees.clone.user) return@listener

            val webhook = WebhookDecree.getWebhook(channel)
            WebhookDecree.sendAs(webhook, event.member!!, event.message.contentRaw)
        }
    }
}

@Serializable
data class CloneState(
    var user: Long = 0,
)