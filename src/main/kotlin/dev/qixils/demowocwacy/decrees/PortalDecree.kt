package dev.qixils.demowocwacy.decrees

import dev.qixils.demowocwacy.decrees.base.WebhookDecree
import net.dv8tion.jda.api.entities.emoji.Emoji
import kotlin.math.floor

class PortalDecree : WebhookDecree(
    "with portals. Now you're thinking",
    Emoji.fromCustom("pblue", 338855230571544576L, false),
    "Ensures thinking with portals is required to read messages"
) {
    override fun alter(content: String): String? {
        val split = content.split(Regex(" +"))
        if (split.count() == 1) return null
        val border = floor(split.count() / 2.0).toInt()
        val topHalf = split.slice(0..<border).joinTo(StringBuilder(), " ")
        val bottomHalf = split.slice(border..<split.size).joinTo(StringBuilder(), " ")
        return "<:pblue:338855230571544576> $bottomHalf. $topHalf <:porange:338855564417302529>"
    }
}