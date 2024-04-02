package dev.qixils.demowocwacy

import dev.qixils.demowocwacy.decrees.*
import kotlinx.serialization.Serializable

@Serializable
data class BotConfig(
    val token: String = "Bot [token]",
    // Guild ID to run the event in
    val guild: Long = 0,
    // Channel ID to send election messages in
    val channel: Long = 0,
    // Channel ID to send messages to the Prime Minister in
    val pmChannel: Long = 0,
    // Channel IDs not effected by event shenanigans
    val protectedChannels: List<Long> = emptyList(),
    // User IDs not effected by event shenanigans
    val protectedUsers: List<Long> = emptyList(),
    // Roles used in the event
    val roles: RoleConfig = RoleConfig(),
    // Configuration for individual decrees
    val decrees: DecreeConfig = DecreeConfig(),
)

@Serializable
data class RoleConfig(
    // Role ID for users that have voted at some point
    val voter: Long = 0,
    // Staff member role IDs
    val staff: List<Long> = emptyList(),
    // Role ID for users that are currently running for office
    val candidate: Long = 0,
    // Role ID for the user that is currently in office
    val currentLeader: Long = 0,
    // Role ID for the user that got 2nd place
    val second: Long = 0,
    // Role ID for users that have previously been in office
    val pastLeader: Long = 0,
    // Role ID for familiars
    val familiar: Long = 0,
)

@Serializable
data class DecreeConfig(
    val unserious: UnseriousConfig = UnseriousConfig(),
    val htc: HTCConfig = HTCConfig(),
    val openai: OpenAIConfig = OpenAIConfig(),
    val stem: STEMConfig = STEMConfig(),
    val disease: EmbraceDiseaseConfig = EmbraceDiseaseConfig(),
    val socialCredit: SocialCreditConfig = SocialCreditConfig(),
    val chaChaSlide: ChaChaSlideConfig = ChaChaSlideConfig(),
    val communism: CommunismConfig = CommunismConfig(),
    val falseDemocracy: FalseDemocracyConfig = FalseDemocracyConfig(),
    val slowmode: SlowmodeConfig = SlowmodeConfig(),
)
