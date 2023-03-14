package dev.qixils.demowocwacy

import kotlinx.serialization.Serializable

@Serializable
data class BotState(
    val selectedDecrees: MutableList<String>,
    val ignoredDecrees: MutableList<String>,
    val election: ElectionState,
) {
    constructor() : this(mutableListOf(), mutableListOf(), ElectionState())
}
