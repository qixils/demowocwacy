package dev.qixils.demowocwacy

import dev.qixils.demowocwacy.decrees.UnseriousState
import kotlinx.serialization.Serializable

@Serializable
data class BotState(
    val selectedDecrees: MutableList<String> = mutableListOf(),
    val ignoredDecrees: MutableList<String> = mutableListOf(),
    val election: ElectionState = ElectionState(),
    val decrees: DecreeState = DecreeState(),
)

@Serializable
data class DecreeState(
    var unserious: UnseriousState = UnseriousState(),
)
