package dev.qixils.demowocwacy

import kotlinx.serialization.Serializable

@Serializable
data class BotState(
    val selectedDecrees: List<String>,
    val ignoredDecrees: List<String>,
) {
    constructor() : this(emptyList(), emptyList())
}
