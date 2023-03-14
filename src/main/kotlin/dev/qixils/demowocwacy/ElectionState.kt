package dev.qixils.demowocwacy

import kotlinx.serialization.Serializable

@Serializable
data class ElectionState(
    val candidates: MutableList<Long>, // list of candidate IDs
    val votes: MutableMap<Long, List<Long>>, // map of voter IDs to approved candidate IDs
) {
    constructor() : this(mutableListOf(), mutableMapOf())
}
