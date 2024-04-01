package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.messages.editMessage
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent

class CommunismDecree : Decree(
    "Communismboard",
    "⚒\uFE0F",
    "Equal starring for everyone",
    true
) {
    private val state: CommunismState
        get() = Bot.state.decrees.communism
    private val starboard: TextChannel
        get() = Bot.guild.getTextChannelById(Bot.config.decrees.communism.starboard)!!
    private val star = Emoji.fromUnicode("⭐")
    private val message = "⭐ There are currently **%,d** stars in the pot."

    override suspend fun execute(init: Boolean) {
        if (state.message == 0L) {
            state.message = starboard.sendMessage(message.format(0)).await().idLong
            Bot.saveState()
        }
        Bot.jda.listener<MessageReactionAddEvent> { event -> coroutineScope {
            if (!isApplicableTo(event.channel)) return@coroutineScope // ignoring the user check since they might not be cached here, and it doesn't really matter
            if (event.reaction.emoji != star) return@coroutineScope

            state.stars += 1
            launch { Bot.saveState() }
            launch { event.reaction.removeReaction(event.retrieveUser().await()).await() }
            launch { starboard.editMessage(state.message.toString(), message.format(state.stars)).await() }
        } }
    }
}

@Serializable
data class CommunismConfig(
    var starboard: Long = 0L,
)

@Serializable
data class CommunismState(
    var message: Long = 0L,
    var stars: Long = 0L,
)