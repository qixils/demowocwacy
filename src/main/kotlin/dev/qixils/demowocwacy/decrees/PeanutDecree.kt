package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.coroutines.await
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.Permission

val peanut = "\uD83E\uDD5C"
val isThatPeanut = IsThatDecree.isThat(peanut)

class PeanutDecree : Decree(
    "Peanuts",
    peanut,
    "peanuts",
    false,
) {
    private val state get() = Bot.state.decrees.peanut

    override suspend fun execute(init: Boolean) {
        Bot.guild.publicRole.manager.revokePermissions(Permission.NICKNAME_CHANGE).await()
        for (member in Bot.guild.loadMembers().await()) {
            if (!Bot.guild.selfMember.canInteract(member)) continue
            val original = member.effectiveName
            if (original == peanut || original == isThatPeanut) continue
            state.originals.computeIfAbsent(member.idLong) { member.nickname ?: "" }
            member.modifyNickname(peanut).await()
        }
        Bot.saveState()
    }

    override suspend fun cleanup() {
        Bot.guild.publicRole.manager.givePermissions(Permission.NICKNAME_CHANGE).await()
        for ((memberId, nickname) in state.originals) {
            val member = try {
                Bot.guild.retrieveMemberById(memberId).await()
            } catch (e: Exception) {
                continue
            }
            val setNickname = if (nickname.isEmpty()) null else nickname
            member.modifyNickname(setNickname).await()
        }
    }
}

@Serializable
data class PeanutState(
    val originals: MutableMap<Long, String> = mutableMapOf(),
)