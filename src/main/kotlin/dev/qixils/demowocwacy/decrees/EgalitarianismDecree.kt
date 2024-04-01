package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.coroutines.await
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.entities.Role

class EgalitarianismDecree : Decree(
    "If noone has power...",
    "\uD83D\uDD32",
    "...then noone has power, duh",
    false
) {
    override suspend fun execute(init: Boolean) {
        val highestRole = Bot.guild.selfMember.roles.first()
        for (role in Bot.guild.roles) {
            if (!highestRole.canInteract(role)) continue
            if (role.colorRaw == Role.DEFAULT_COLOR_RAW) continue
            Bot.state.decrees.egalitarianism.colors[role.idLong] = role.colorRaw
            role.manager.setColor(Role.DEFAULT_COLOR_RAW).await()
        }
    }

    override suspend fun cleanup() {
        val highestRole = Bot.guild.selfMember.roles.first()
        for ((roleId, color) in Bot.state.decrees.egalitarianism.colors) {
            val role = Bot.guild.getRoleById(roleId) ?: continue
            if (!highestRole.canInteract(role)) continue
            role.manager.setColor(color).await()
        }
    }
}

@Serializable
data class EgalitarianismState(
    val colors: MutableMap<Long, Int> = mutableMapOf(),
)