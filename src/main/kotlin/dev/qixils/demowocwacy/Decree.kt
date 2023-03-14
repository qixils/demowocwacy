package dev.qixils.demowocwacy

import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.Channel
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel

abstract class Decree(
    val name: String,
    val emoji: String,
    val description: String,
    /**
     * Whether this decree is persistent, meaning its execute function should be called on every startup.
     */
    val persistent: Boolean = false,
) {
    fun displayName() = "$emoji $name"
    abstract fun execute()

    fun isApplicableTo(channel: Channel, author: User): Boolean {
        if (channel is PrivateChannel) return false
        if (channel.idLong in Bot.config.protectedChannels) return false
        if (author.idLong in Bot.config.protectedUsers) return false
        return true
    }

    open fun cleanup() {
    }
}