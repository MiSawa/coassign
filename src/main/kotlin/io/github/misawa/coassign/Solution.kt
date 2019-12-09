package io.github.misawa.coassign

import kotlin.math.max

class Solution(
    private val graph: BipartiteGraph,
    private val used: BooleanArray,
    private val potential: WeightArray
) {
    val value: LargeWeight

    init {
        var res: LargeWeight = 0
        for (e in graph.forwardEdges) if (used[e]) res += graph.weights[e]
        value = res
    }

    fun getMatches(): List<Pair<Int, Int>> = graph.forwardEdges.filter { used[it] }.map {
        if (graph.flipped) {
            ((graph.targets[it] - graph.lSize) to graph.sources[it])
        } else {
            (graph.sources[it] to (graph.targets[it] - graph.lSize))
        }
    }

    fun checkConstraints() {
        val matchCount = IntArray(graph.numV)
        for (e in graph.forwardEdges) if (used[e]) {
            ++matchCount[graph.sources[e]]
            ++matchCount[graph.targets[e]]
        }
        for (u in graph.nodes) check(matchCount[u] <= graph.multiplicities[u]) { "The matching includes too many edges adjacent to $u" }
    }

    fun checkOptimality() {
        for (e in graph.edges) {
            val rWeight = graph.weights[e] - potential[graph.sources[e]] + potential[graph.targets[e]]
            if (rWeight > 0) check(used[e]) { "There's an edge that has positive residual weight but not used" }
            if (rWeight < 0) check(!used[e]) { "There's an edge that has negative residual weight but used" }
        }
        fun sgn(u: Node): Int = if (u < graph.lSize) 1 else -1
        var dual: LargeWeight = 0
        for (u in graph.nodes) dual += graph.multiplicities[u] * max(0, sgn(u) * potential[u]).toLargeWeight()
        for (e in graph.forwardEdges) dual += max(
            0,
            graph.weights[e] - potential[graph.sources[e]] + potential[graph.targets[e]]
        )
        check(value == dual) { "Primal and dual value should be the same. Primal: $value, dual: $dual" }
    }

    fun check() {
        checkConstraints()
        checkOptimality()
    }
}