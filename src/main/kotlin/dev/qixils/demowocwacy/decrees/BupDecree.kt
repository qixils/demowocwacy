package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.coroutines.await
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.entities.emoji.Emoji

class BupDecree : Decree(
    "BUP",
    Emoji.fromCustom("bup", 599645812766539779L, false),
    "I am BUP. You are BUP. We are BUP.",
    false
) {
    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun execute(init: Boolean) {
        if (Bot.config.decrees.bup.role == 0L) {
            Bot.logger.warn("Missing BUP role")
            return
        }
        val role = Bot.guild.getRoleById(Bot.config.decrees.bup.role)
        if (role == null) {
            Bot.logger.warn("Can't find BUP role")
            return
        }
        if (!Bot.guild.selfMember.canInteract(role)) {
            Bot.logger.warn("Can't touch BUP role")
            return
        }
        for (member in Bot.guild.loadMembers().await()) {
            try {
                Bot.guild.addRoleToMember(member, role).await()
            } catch (e: Exception) {
                Bot.logger.warn("Could not grant bup role to ${member.id}", e)
            }
        }
    }
}

@Serializable
data class BupConfig(
    val role: Long = 0,
)