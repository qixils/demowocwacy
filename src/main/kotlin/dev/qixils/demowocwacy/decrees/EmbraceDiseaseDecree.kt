package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.listener
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class EmbraceDiseaseDecree : Decree(
    "Embrace Disease",
    "\uD83E\uDDA0",
    "Be careful where you cough lest you spread Baloomba-19",
    true
) {
    override val priority: Int
        get() = 10
    private fun getRole() = Bot.guild.getRoleById(Bot.config.decrees.disease.role)!!

    override suspend fun execute(init: Boolean) {
        if (init) {
            var target = Bot.state.election.primeMinister
            if (target == 0L)
                target = Bot.guild.findMembersWithRoles(Bot.familiar).await().random().idLong
            Bot.guild.addRoleToMember(UserSnowflake.fromId(target), getRole()).await()
        }
        Bot.jda.listener<MessageReceivedEvent> { event ->
            val member = event.member ?: return@listener

            val role = getRole()
            if (role !in member.roles) return@listener

            val mentions = event.message.mentions.users.toMutableList()

            try { event.message.messageReference?.resolve()?.await()?.author?.let { mentions.add(it) } }
            catch (e: Exception) { Bot.logger.warn("Failed to resolve referenced message", e) }

            for (user in mentions) {
                if (Bot.guild.getMember(user)?.let { role in it.roles } == true) continue
                try {
                    Bot.guild.addRoleToMember(user, role).await()
                } catch (e: Exception) {
                    Bot.logger.warn("Failed to infect user ${user.id}", e)
                }
            }
        }
    }
}

@Serializable
data class EmbraceDiseaseConfig(
    val role: Long = 0,
)