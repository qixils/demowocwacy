package dev.qixils.demowocwacy

import dev.qixils.demowocwacy.decrees.ChatGPTDecree
import net.dv8tion.jda.api.entities.Message
import java.util.regex.Matcher
import java.util.regex.Pattern

fun String.truncate(limit: Int): String {
    if (length > limit)
        return take(limit - 1) + '…'
    return this
}

enum class UserDisplay {
    MEMBER,
    USER,
    CHATGPT,
    NONE,
}

fun Message.getDisplayContent(
    users: UserDisplay = UserDisplay.MEMBER,
    emojis: Boolean = true,
    channels: Boolean = true,
    roles: Boolean = true,
): String {
    var tmp: String = contentRaw
    if (users != UserDisplay.NONE) {
        for (user in mentions.users) {
            var name = if (users == UserDisplay.MEMBER && hasGuild() && guild.isMember(user))
                guild.getMember(user)!!.effectiveName
            else
                user.name
            if (users == UserDisplay.CHATGPT)
                name = user.name.replace(ChatGPTDecree.nameFilter, "_")
            tmp = tmp.replace(
                ("<@!?" + Pattern.quote(user.id) + '>').toRegex(),
                '@'.toString() + Matcher.quoteReplacement(name)
            )
        }
    }
    if (emojis) {
        for (emoji in mentions.customEmojis) {
            tmp = tmp.replace(emoji.asMention, ":" + emoji.name + ":")
        }
    }
    if (channels) {
        for (mentionedChannel in mentions.channels) {
            tmp = tmp.replace(mentionedChannel.asMention, '#'.toString() + mentionedChannel.name)
        }
    }
    if (roles) {
        for (mentionedRole in mentions.roles) {
            tmp = tmp.replace(mentionedRole.asMention, '@'.toString() + mentionedRole.name)
        }
    }
    return tmp
}