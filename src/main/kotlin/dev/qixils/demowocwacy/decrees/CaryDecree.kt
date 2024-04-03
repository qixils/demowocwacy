package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.listener
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import dev.qixils.demowocwacy.decrees.base.WebhookDecree
import net.dv8tion.jda.api.entities.channel.attribute.IWebhookContainer
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.util.*

class CaryDecree : Decree(
    "Return of the King",
    Emoji.fromCustom("carypog", 849418300777299988L, false),
    "Reinstate Cary as the King",
    true
) {
    private val random = Random()

    override suspend fun execute(init: Boolean) {
        Bot.jda.listener<MessageReceivedEvent> { event ->
            if (!isApplicableTo(event.message)) return@listener
            val channel = event.channel
            if (channel !is IWebhookContainer) return@listener
            if (random.nextInt(50) != 0) return@listener

            val webhook = WebhookDecree.getWebhook(channel)
            webhook.sendMessage("hi guys i'm cary")
                .setUsername("carykh")
                .setAvatarUrl("https://qixils.us-east-1.linodeobjects.com/carykh.png")
                .await()
        }
    }
}