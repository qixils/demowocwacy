package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.coroutines.await
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.Permission

val peanut = "\uD83E\uDD5C"

class PeanutDecree : Decree(
    "Peanuts",
    peanut,
    "peanuts",
    true,
) {
    override suspend fun execute(init: Boolean) {
        if (init) {
            Bot.guild.publicRole.manager.revokePermissions(Permission.NICKNAME_CHANGE).await()
        }
        for (member in Bot.guild.loadMembers().await()) {
            if (!Bot.guild.selfMember.canInteract(member)) continue
            val original = member.nickname
            if (original == peanut) continue
            if (original != null) {
                Bot.state.decrees.peanut.originals[member.idLong] = original
            }
            member.modifyNickname(peanut).await()
        }
    }

    override suspend fun cleanup() {
        Bot.guild.publicRole.manager.givePermissions(Permission.NICKNAME_CHANGE).await()
        for ((memberId, nickname) in Bot.state.decrees.peanut.originals) {
            val member = try {
                Bot.guild.retrieveMemberById(memberId).await()
            } catch (e: Exception) {
                continue
            }
            member.modifyNickname(nickname).await()
        }
    }
}

@Serializable
data class PeanutState(
    val originals: MutableMap<Long, String> = mutableMapOf(),
)