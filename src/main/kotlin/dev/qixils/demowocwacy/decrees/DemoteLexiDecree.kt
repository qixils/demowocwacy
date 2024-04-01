package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.coroutines.await
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree

class DemoteLexiDecree : Decree(
    "Demote Lexi",
    "\u2692\uFE0F",
    "Vanquishes the final staff member of a bygone era",
    false,
) {
    override suspend fun execute(init: Boolean) {
        val guild = Bot.guild
        val lexi = guild.retrieveOwner().await()
        guild.modifyMemberRoles(lexi, emptyList(), Bot.config.roles.staff.mapNotNull { guild.getRoleById(it) }).await()
    }
}