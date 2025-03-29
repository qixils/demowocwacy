package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.interactions.commands.Command
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent

class FruitGameDecree : Decree(
    "Fruit Game",
    "🍉",
    "Play an enticing game of fruits underneath 9.0's messages",
    true,
) {
    companion object {
        private val fruits = mapOf(
            "👻" to -200L,
            "🍒" to 100L,
            "🍓" to 300L,
            "🍊" to 500L,
            "🍎" to 700L,
            "🍈" to 1000L,
            "👾" to 2000L,
            "🔔" to 3000L,
            "🔑" to 5000L,
        )
    }

    private val state get() = Bot.state.decrees.fruit

    override suspend fun execute(init: Boolean) {
        Bot.jda.upsertCommand(Command("fruits", "Tally the score of the fruits you've collected!")).await()

        // leaderboard
        Bot.jda.listener<SlashCommandInteractionEvent> { event -> coroutineScope {
            if (event.fullCommandName != "fruits") return@coroutineScope
            val reply = async { event.deferReply(true).await() }
            val tally = state.users.mapValues { it.value.score }
            val top = tally.entries
                .sortedByDescending { it.value }
                .takeWhile { it.value > 0L }
                .take(10)
            val content = buildString {
                if (tally.isEmpty()) {
                    append("Nobody has yet collected any fruit.")
                } else {
                    append("The top ${top.size} users with the most fruit:\n")
                    top.forEachIndexed { index, entry ->
                        append("${index}. <@${entry.key}>, ${entry.value} points\n")
                    }
                }

                appendLine()

                val score = state.users[event.user.idLong]?.score ?: 0L
                val plural = if (score != 1L) "s" else ""
                append("You have $score point$plural.")
            }
            reply.await().editOriginal(content).await()
        } }

        Bot.jda.listener<MessageReceivedEvent> { event ->
            if (!isApplicableTo(event.message)) return@listener
            if (event.message.type.isSystem) return@listener
            if (event.author.idLong != Bot.config.decrees.fruit.ninepointoh) return@listener

            val fruit = fruits.keys.random()
            event.message.addReaction(Emoji.fromUnicode(fruit)).await()
        }

        Bot.jda.listener<MessageReactionAddEvent> { event ->
            if (event.emoji !is UnicodeEmoji) return@listener
            val score = fruits[event.emoji.formatted] ?: return@listener

            val reactor = event.retrieveUser().await()
            if (reactor.isBot == true) return@listener

            val message = event.retrieveMessage().await()
            if (!isApplicableTo(message)) return@listener
            if (message.author.idLong != Bot.config.decrees.fruit.ninepointoh) return@listener

            val fruitUser = state.users.computeIfAbsent(reactor.idLong) { FruitUser() }
            if (!fruitUser.messages.add(message.idLong)) return@listener

            fruitUser.score += score
            Bot.saveState()
            // Fun fact: anyone can add fruit to any 9.0 message for free points
            // I don't think I'll fix it
        }
    }
}

@Serializable
data class FruitConfig(
    val ninepointoh: Long = 0,
)

@Serializable
data class FruitUser(
    var messages: MutableSet<Long> = mutableSetOf(),
    var score: Long = 0,
)

@Serializable
data class FruitState(
    var users: MutableMap<Long, FruitUser> = mutableMapOf(),
)