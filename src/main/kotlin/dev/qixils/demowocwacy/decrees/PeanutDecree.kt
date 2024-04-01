package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.coroutines.await
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.Permission

class PeanutDecree : Decree(
    "Peanuts",
    "\uD83E\uDD5C",
    "peanuts",
    true,
) {
    override suspend fun execute(init: Boolean) {
        Bot.guild.publicRole.manager.revokePermissions(Permission.NICKNAME_CHANGE).await()
        for (member in Bot.guild.members) {
            val original = member.nickname
            if (original != null) {
                Bot.state.decrees.peanut.originals[member.idLong] = original
            }
            member.modifyNickname("\uD83E\uDD5C").await()
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