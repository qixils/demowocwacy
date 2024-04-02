package dev.qixils.demowocwacy.decrees

import dev.minn.jda.ktx.coroutines.await
import dev.qixils.demowocwacy.Bot
import dev.qixils.demowocwacy.Decree
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromStream
import net.dv8tion.jda.api.entities.UserSnowflake

class SocialCreditDecree : Decree(
    "Social Credit",
    "⭐",
    "Divide commoners into classes based on star wealth",
    false
) {
    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun execute(init: Boolean) {
        val leaderboard = withContext(Dispatchers.IO) {
            Bot.json.decodeFromStream<Map<String, Int>>(javaClass.getResourceAsStream("/stars.json")!!)
        }
        val roles = Bot.config.decrees.socialCredit.roles
            .mapNotNull { (Bot.guild.getRoleById(it.id) ?: return@mapNotNull null) to it.amount }
        for ((userId, stars) in leaderboard) {
            val role = roles.lastOrNull { it.second <= stars } ?: continue
            try {
                val id = UserSnowflake.fromId(userId)
                if (Bot.guild.getMember(id)?.let{ role.first in it.roles } == true) continue // kinda unnecessary ngl
                Bot.guild.addRoleToMember(id, role.first).await()
            } catch (e: Exception) {
                Bot.logger.warn("Could not grant star role to $userId")
            }
        }
    }
}

@Serializable
data class SocialCreditRole(
    val id: Long = 0,
    val amount: Int = 1,
)

@Serializable
data class SocialCreditConfig(
    val roles: List<SocialCreditRole> = listOf(SocialCreditRole()),
)