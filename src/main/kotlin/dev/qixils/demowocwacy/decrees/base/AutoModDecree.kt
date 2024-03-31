package dev.qixils.demowocwacy.decrees.base

import dev.minn.jda.ktx.coroutines.await
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree

abstract class AutoModDecree(name: String, emoji: String, description: String) : Decree(name, emoji, description, false) {
    override suspend fun execute() {
        val rules = Bot.guild.retrieveAutoModRules().await()
        val common = rules.find { it.name == name }
        if (common == null) {
            Bot.logger.warn("Could not find `$name` rule to enable it")
            return
        }
        common.manager.setEnabled(true).await()
    }
}