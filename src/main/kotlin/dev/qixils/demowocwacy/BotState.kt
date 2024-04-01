package dev.qixils.demowocwacy

import dev.qixils.demowocwacy.decrees.CloneState
import dev.qixils.demowocwacy.decrees.UnseriousState
import kotlinx.serialization.Serializable

@Serializable
data class BotState(
    var selectedDecrees: MutableList<String> = mutableListOf(),
    var ignoredDecrees: MutableList<String> = mutableListOf(),
    var election: ElectionState = ElectionState(),
    var decrees: DecreeState = DecreeState(),
    var nextTask: NextTask = NextTask.OPEN_REGISTRATION,
)

@Serializable
data class DecreeState(
    val unserious: UnseriousState = UnseriousState(),
    val clone: CloneState = CloneState(),
)

enum class NextTask {
    OPEN_REGISTRATION,
    // todo
}
