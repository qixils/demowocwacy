package dev.qixils.demowocwacy

import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.Channel
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.entities.emoji.Emoji

abstract class Decree(
    val name: String,
    val emoji: Emoji,
    val description: String,
    /**
     * Whether this decree is persistent, meaning its execute function should be called on every startup.
     */
    val persistent: Boolean = false,
) {
    constructor(name: String, emoji: String, description: String, persistent: Boolean = false) : this(name, Emoji.fromFormatted(emoji), description, persistent)

    val displayName: String
        get() = "${emoji.formatted} $name"

    abstract fun execute()

    fun isApplicableTo(channel: Channel, author: User): Boolean {
        if (channel !is GuildChannel) return false
        if (!Bot.isInGuild(channel)) return false
        if (channel.idLong in Bot.config.protectedChannels) return false
        if (author.idLong in Bot.config.protectedUsers) return false
        return true
    }

    open fun cleanup() {
    }
}