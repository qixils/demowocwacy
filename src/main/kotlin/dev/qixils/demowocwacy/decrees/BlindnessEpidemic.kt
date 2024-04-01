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
    override suspend fun execute(init: Boolean) {
        val override = Bot.unserious.getPermissionOverride(Bot.guild.publicRole)
        val allow = override?.allowed ?: emptySet()
        val deny = (override?.denied ?: emptySet()) + Permission.MESSAGE_HISTORY
        Bot.unserious.manager.putPermissionOverride(Bot.guild.publicRole, allow, deny).await()
    }

    override suspend fun cleanup() {
        val override = Bot.unserious.getPermissionOverride(Bot.guild.publicRole)
        val allow = override?.allowed ?: emptySet()
        val deny = (override?.denied ?: emptySet()) - Permission.MESSAGE_HISTORY
        Bot.unserious.manager.putPermissionOverride(Bot.guild.publicRole, allow, deny).await()
    }
}