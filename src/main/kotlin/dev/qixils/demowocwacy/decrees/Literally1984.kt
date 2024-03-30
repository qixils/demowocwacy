package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.coroutines.await
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree

class Literally1984 : Decree(
    "Literally 1984",
    "\uD83D\uDE94",
    "Maximize auto-moderator sensitivity",
    false
) {
    override suspend fun execute() {
        val rules = Bot.guild.retrieveAutoModRules().await()
        val common = rules.find { it.name == "Commonly Flagged Words" }
        if (common == null) {
            Bot.logger.warn("Could not find `Commonly Flagged Words` rule to enable it")
            return
        }
        common.manager.setEnabled(true).await()
    }
}