package dev.qixils.demowocwacy.decrees

import dev.qixils.demowocwacy.decrees.base.WebhookDecree

class NoMathDecree : WebhookDecree(
    "Ban Math",
    "*\uFE0F⃣",
    "Censor all discussion of mathematics"
) {
    private val pattern = Regex("[0-9+/*-]")

    override fun alter(content: String): String? {
        val censored = pattern.replace(content, "·")
        if (content == censored) return null
        return censored
    }
}