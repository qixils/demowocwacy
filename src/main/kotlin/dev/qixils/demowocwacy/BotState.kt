package dev.qixils.demowocwacy

import dev.qixils.demowocwacy.decrees.UnseriousState
import kotlinx.serialization.Serializable

@Serializable
data class BotState(
    var selectedDecrees: MutableList<String> = mutableListOf(),
    var ignoredDecrees: MutableList<String> = mutableListOf(),
    var election: ElectionState = ElectionState(),
    var decrees: DecreeState = DecreeState(),
)

@Serializable
data class DecreeState(
    var unserious: UnseriousState = UnseriousState(),
)
