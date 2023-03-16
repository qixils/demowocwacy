package dev.qixils.demowocwacy

import kotlinx.serialization.Serializable

@Serializable
data class ElectionState(
    val candidates: MutableList<Long>, // list of candidate IDs
    val candidateVotes: MutableMap<Long, List<Long>>, // map of voter IDs to approved candidate IDs
    val decreeVotes: MutableMap<Long, String>, // map of voter IDs to chosen decree name
    val decrees: List<String>, // the names of decrees being voted on in this election
) {
    constructor() : this(mutableListOf(), mutableMapOf(), mutableMapOf(), emptyList())
}
