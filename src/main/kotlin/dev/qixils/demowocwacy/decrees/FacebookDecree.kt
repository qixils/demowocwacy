package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.interactions.commands.Command
import dev.minn.jda.ktx.interactions.commands.option
import dev.minn.jda.ktx.interactions.commands.subcommand
import dev.minn.jda.ktx.interactions.components.getOption
import dev.minn.jda.ktx.messages.reply_
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class FacebookDecree : Decree(
    "Facebook",
    "\uD83D\uDCD4",
    "Lexi sent you a friend request! Accept?",
    true
) {
    private val state get() = Bot.state.decrees.facebook

    private val friendNotif = "You are now friends with %s!"
    private val inc = { _: Long, value: Int? -> (value ?: 0) + 1 }

    private fun pair(user1: Long, user2: Long): Pair<Long, Long> {
        val compare = user1 < user2
        val left  = if (compare) user1 else user2
        val right = if (compare) user2 else user1
        return left to right
    }

    private fun friendOf(user: Long, pair: Pair<Long, Long>): Long {
        if (pair.first == user)
            return pair.second
        return pair.first
    }

    private suspend fun tryMessage(member: User, content: String) {
        try {
            member.openPrivateChannel().await().sendMessage(content).await()
        } catch (ignored: Exception) {
            Bot.logger.warn("Failed to message $member\n$content")
        }
    }

    override suspend fun execute(init: Boolean) {
        Bot.jda.upsertCommand(Command("friends", "Manage your friends list") {
            subcommand("add", "Ask someone to be your friend or accept their request") {
                option<User>("member", "The member to invite", true)
            }
            subcommand("list", "Lists your friends and pending requests") {
                option<User>("member", "The user to view the friends list of if not yourself", false)
            }
            subcommand("remove", "Removes a friend or denies a pending friend request") {
                option<User>("member", "The member to unfriend", true)
            }
            subcommand("leaderboard", "Lists the users with the most friends")
        }).await()

        // add
        Bot.jda.listener<SlashCommandInteractionEvent> { event -> coroutineScope {
            if (event.fullCommandName != "friends add") return@coroutineScope
            val member = event.getOption<User>("member") ?: run {
                event.reply_("That user is not on ${Bot.guild.name}", ephemeral = true).await()
                return@coroutineScope
            }
            val reply = async { event.deferReply(true).await() }
            val friendPair = pair(event.user.idLong, member.idLong)
            if (state.friends.contains(friendPair)) {
                reply.await().editOriginal("You are already friends with ${member.asMention}!").await()
            } else if (state.requests.remove(member.idLong to event.user.idLong)) {
                state.friends.add(friendPair)
                launch { Bot.saveState() }
                launch { reply.await().editOriginal(friendNotif.format(member.asMention)).await() }
                launch { tryMessage(member, friendNotif.format(event.user.asMention)) }
            } else if (state.requests.add(event.user.idLong to member.idLong)) {
                launch { Bot.saveState() }
                launch { reply.await().editOriginal("Friend request sent to ${member.asMention}.").await() }
                launch { tryMessage(member, "${event.user.asMention} sent you a friend request! Use `/friends add` to accept it.") }
            } else {
                reply.await().editOriginal("You have already sent this user a friend request!").await()
            }
        } }

        // list
        Bot.jda.listener<SlashCommandInteractionEvent> { event -> coroutineScope {
            if (event.fullCommandName != "friends list") return@coroutineScope
            val member = event.getOption<User>("member") ?: event.user
            val reply = async { event.deferReply(true).await() }

            val friends = state.friends
                .filter { it.first == event.user.idLong || it.second == event.user.idLong }
                .map { friendOf(event.user.idLong, it) }

            if (member != event.user) {
                val content = buildString {
                    append(member.asMention)
                    if (friends.isEmpty()) {
                        append(" does not have any friends.")
                    } else {
                        append(" is friends with ")
                        friends.joinTo(this, postfix = ".") { "<@$it>" }
                    }
                }
                reply.await().editOriginal(content).await()
                return@coroutineScope
            }

            val incoming = state.requests
                .filter { it.second == event.user.idLong }
                .map { it.first }

            val outgoing = state.requests
                .filter { it.first == event.user.idLong }
                .map { it.second }

            val content = buildString {
                append("## Friends\n")
                if (friends.isEmpty()) {
                    append("You do not have any friends.\n")
                } else {
                    append("You are friends with ")
                    friends.joinTo(this, postfix = ".\n") { "<@$it>" }
                }

                append("## Incoming Requests\n")
                if (incoming.isEmpty()) {
                    append("You do not have any incoming friend requests.\n")
                } else {
                    append("You have incoming friend requests from ")
                    incoming.joinTo(this, postfix = ".\n") { "<@$it>" }
                }

                append("## Outgoing Requests\n")
                if (outgoing.isEmpty()) {
                    append("You do not have any outgoing friend requests.\n")
                } else {
                    append("You have outgoing friend requests to ")
                    outgoing.joinTo(this, postfix = ".\n") { "<@$it>" }
                }
            }

            reply.await().editOriginal(content).await()
        } }

        // remove
        Bot.jda.listener<SlashCommandInteractionEvent> { event -> coroutineScope {
            if (event.fullCommandName != "friends remove") return@coroutineScope
            val member = event.getOption<User>("member") ?: run {
                event.reply_("That user is not on ${Bot.guild.name}", ephemeral = true).await()
                return@coroutineScope
            }
            val reply = async { event.deferReply(true).await() }
            if (state.requests.remove(event.user.idLong to member.idLong)) {
                launch { Bot.saveState() }
                launch { reply.await().editOriginal("Your request to be ${member.asMention}'s friend has been cancelled.").await() }
            } else if (state.requests.remove(member.idLong to event.user.idLong)) {
                launch { Bot.saveState() }
                launch { reply.await().editOriginal("${member.asMention}'s request to be friends has been rejected.").await() }
            } else if (state.friends.remove(pair(event.user.idLong, member.idLong))) {
                launch { Bot.saveState() }
                launch { reply.await().editOriginal("${member.asMention} has been removed from your friends list.").await() }
            } else {
                reply.await().editOriginal("You are not friends with this user, and neither of you have sent each other friend requests.").await()
            }
        }}

        // leaderboard
        Bot.jda.listener<SlashCommandInteractionEvent> { event -> coroutineScope {
            if (event.fullCommandName != "friends leaderboard") return@coroutineScope
            val reply = async { event.deferReply(true).await() }
            val tally = mutableMapOf<Long, Int>()
            for ((user1, user2) in state.friends) {
                tally.compute(user1, inc)
                tally.compute(user2, inc)
            }
            val top = tally.entries
                .sortedByDescending { it.value }
                .take(10)
            val content = buildString {
                if (tally.isEmpty()) {
                    append("Nobody has any friends.")
                } else {
                    append("The top ${top.size} users with the most friends:\n")
                    top.forEachIndexed { index, entry ->
                        append("${index}. <@${entry.key}>, ${entry.value} friends\n")
                    }
                }
            }.trimEnd()
            reply.await().editOriginal(content).await()
        } }
    }
}

@Serializable
data class FacebookState(
    val requests: MutableSet<Pair<Long, Long>> = mutableSetOf(),
    val friends: MutableSet<Pair<Long, Long>> = mutableSetOf(),
)