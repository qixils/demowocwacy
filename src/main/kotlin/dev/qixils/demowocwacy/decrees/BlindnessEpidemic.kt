package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.coroutines.await
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import net.dv8tion.jda.api.Permission

class BlindnessEpidemic : Decree(
    "Blindness Epidemic",
    "\uD83D\uDE48",
    "Reduce the visibility of messages",
    false
) {
    override suspend fun execute() {
        Bot.unserious.manager.putPermissionOverride(Bot.guild.publicRole, emptySet(), setOf(Permission.MESSAGE_HISTORY)).await()
    }
}