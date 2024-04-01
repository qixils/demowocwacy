package dev.qixils.demowocwacy

import dev.qixils.demowocwacy.decrees.*
import kotlinx.serialization.Serializable

@Serializable
data class BotState(
    var selectedDecrees: MutableList<String> = mutableListOf(),
    var ignoredDecrees: MutableList<String> = mutableListOf(),
    var election: ElectionState = ElectionState(),
    var decrees: DecreeState = DecreeState(),
    var nextTask: Task = Task.OPEN_REGISTRATION,
)

@Serializable
data class DecreeState(
    val unserious: UnseriousState = UnseriousState(),
    val clone: CloneState = CloneState(),
    val veto: VetoState = VetoState(),
    val chaChaSlide: ChaChaSlideState = ChaChaSlideState(),
    val communism: CommunismState = CommunismState(),
    val egalitarianism: EgalitarianismState = EgalitarianismState(),
    val facebook: FacebookState = FacebookState(),
)

enum class Task {
    OPEN_REGISTRATION,
    OPEN_BALLOT,
    CLOSE_BALLOT,
    CLOSE_TIEBREAK,
    WELCOME_PM,
    PM_TIMEOUT,
    GOODBYE,
    SLEEP,
}
