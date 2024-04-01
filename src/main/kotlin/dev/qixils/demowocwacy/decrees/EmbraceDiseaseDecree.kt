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
    "Be careful where you cough",
    true
) {
    private val pattern = Regex("<@!?(\\d{17,19})>")
    private fun getRole() = Bot.guild.getRoleById(Bot.config.decrees.disease.role)!!

    override suspend fun execute(init: Boolean) {
        if (init) {
            var target = Bot.state.election.primeMinister
            if (target == 0L)
                target = Bot.guild.findMembersWithRoles(Bot.familiar).await().random().idLong
            Bot.guild.addRoleToMember(UserSnowflake.fromId(target), getRole()).await()
        }
        Bot.jda.listener<MessageReceivedEvent> { event ->
            if (!isApplicableTo(event.channel, event.author)) return@listener
            val role = getRole()
            if (role !in event.member!!.roles) return@listener

            val matches = pattern.findAll(event.message.contentRaw)
            for (match in matches) {
                val user = UserSnowflake.fromId(match.groupValues[1])
                try {
                    Bot.guild.addRoleToMember(user, role)
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