package dev.qixils.demowocwacy

import kotlinx.serialization.Serializable

@Serializable
data class ElectionState(
    val candidates: MutableSet<Long> = mutableSetOf(), // list of candidate IDs
    val candidateVotes: MutableMap<Long, List<Long>> = mutableMapOf(), // map of voter IDs to approved candidate IDs
    val tieBreakCandidates: MutableSet<Long> = mutableSetOf(), // list of winning candidate IDs
    val tieBreakVotes: MutableMap<Long, Long> = mutableMapOf(), // map of voter IDs to candidate IDs to break ties
    val decreeVotes: MutableMap<Long, String> = mutableMapOf(), // map of voter IDs to chosen decree name
    var decrees: List<String> = emptyList(), // the names of decrees being voted on in this election
)
