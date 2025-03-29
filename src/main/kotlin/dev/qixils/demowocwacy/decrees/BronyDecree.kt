package dev.qixils.demowocwacy.decrees

import dev.qixils.demowocwacy.decrees.base.WebhookDecree

class BronyDecree : WebhookDecree(
    "So Long, Everypony",
    "\uD83D\uDC0E",
    "Discussions of bronies and pegasisters are welcome no longer"
) {
    private val pattern = Regex("((p|br)on(y|ie)s?|pegasis(ters?)?)", RegexOption.IGNORE_CASE)

    override fun alter(content: String): String? {
        val censored = pattern.replace(content, "*")
        if (content == censored) return null
        return censored
    }
}