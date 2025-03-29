package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.coroutines.await
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import net.dv8tion.jda.api.Permission

class IsThatDecree : Decree(
    "Is that...?",
    "💭",
    "Could that be Sažo? Or, no, could that be someone else?",
    false,
) {
    companion object {
        const val PREFIX = "Is that "
        const val SUFFIX = "?"
        const val TEXT_SIZE = PREFIX.length + SUFFIX.length
        const val LEFTOVER = 32 - TEXT_SIZE
        const val TRUNCATED = LEFTOVER - 1

        fun isThat(name: String): String {
            val trimmed = if (name.length > LEFTOVER) (name.substring(0..<TRUNCATED) + '…') else name
            return "$PREFIX$trimmed$SUFFIX"
        }
    }

    private val state get() = Bot.state.decrees.peanut

    override suspend fun execute(init: Boolean) {
        Bot.guild.publicRole.manager.revokePermissions(Permission.NICKNAME_CHANGE).await()
        for (member in Bot.guild.loadMembers().await()) {
            if (!Bot.guild.selfMember.canInteract(member)) continue
            val original = member.effectiveName
            val trimmed = if (original.length > LEFTOVER) (original.substring(0..<TRUNCATED) + '…') else original
            val newName = "$PREFIX$trimmed$SUFFIX"
            state.originals.computeIfAbsent(member.idLong) { member.nickname ?: "" }
            member.modifyNickname(newName).await()
        }
        Bot.saveState()
    }

    override suspend fun cleanup() {
        Bot.guild.publicRole.manager.givePermissions(Permission.NICKNAME_CHANGE).await()
        for ((memberId, nickname) in state.originals) {
            val member = try {
                Bot.guild.retrieveMemberById(memberId).await()
            } catch (_: Exception) {
                continue
            }
            val setNickname = if (nickname.isEmpty()) null else nickname
            member.modifyNickname(setNickname).await()
        }
    }
}