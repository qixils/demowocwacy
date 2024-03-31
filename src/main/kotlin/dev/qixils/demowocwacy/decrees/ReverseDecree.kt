package dev.qixils.demowocwacy.decrees

import dev.qixils.demowocwacy.decrees.base.WebhookDecree

class ReverseDecree : WebhookDecree(
    "esreveR",
    "\uD83D\uDD01",
    "Reverses all sent messages"
) {
    override fun alter(content: String): String? {
        if (content.length <= 1) return null
        return content.reversed()
    }
}