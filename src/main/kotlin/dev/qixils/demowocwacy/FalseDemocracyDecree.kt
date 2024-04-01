package dev.qixils.demowocwacy

import dev.minn.jda.ktx.coroutines.await
import kotlinx.serialization.Serializable

class FalseDemocracyDecree : Decree(
    "If everyone has power...",
    "\uD83D\uDC51",
    "...then noone has power",
    false
) {
    private fun getRole() = Bot.guild.getRoleById(Bot.config.decrees.falseDemocracy.role)!!
    override suspend fun execute(init: Boolean) {
        val role = getRole()
        for (member in Bot.guild.loadMembers().await()) {
            Bot.guild.addRoleToMember(member, role).await()
        }
    }

    override suspend fun cleanup() {
        getRole().delete().await()
    }
}

@Serializable
data class FalseDemocracyConfig(
    val role: Long = 0L,
)