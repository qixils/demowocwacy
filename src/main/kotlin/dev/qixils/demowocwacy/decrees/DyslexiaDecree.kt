package dev.qixils.demowocwacy.decrees

import dev.qixils.demowocwacy.decrees.base.WebhookDecree
import java.util.*

class DyslexiaDecree : WebhookDecree(
    "Dyslexia",
    "\uD83D\uDE16",
    "Intlicfs bisadility upon all bembers"
) {
    private val random = Random()
    private val set = "bdpq"
    override fun alter(content: String): String {
        return buildString {
            for (char in content) {
                if (char in set && random.nextInt(7) == 0)
                    append(set.random())
                else
                    append(char)
            }
        }
    }
}