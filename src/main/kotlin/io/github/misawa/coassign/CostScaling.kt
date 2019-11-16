package io.github.misawa.coassign

import kotlin.math.roundToInt

private typealias Node = Int
private typealias Edge = Int

//@ExperimentalStdlibApi
class CostScaling(
    private val params: Params,
    private val graph: BipartiteGraph
) {
    private val initialScale = (graph.numV + 2) * params.scalingFactor
    private val numV: Int = graph.numV
    private val numE: Int = graph.numE
    private val lSize: Int = graph.lSize
    private val rSize: Int = graph.rSize
    private val flipped: Boolean = graph.flipped
    private val edgeStarts: IntArray = graph.edgeStarts
    private val sources: IntArray = graph.sources
    private val targets: IntArray = graph.targets
    private val reverse: IntArray = graph.reverse
    private val weights: LargeWeightArray =
        LargeWeightArray(graph.weights.size) { initialScale * graph.weights[it].toLargeWeight() }

    private val minExcess: IntArray = IntArray(numV) { u ->
        if (u < lSize) -graph.multiplicities[u]
        else 0
    }
    private val maxExcess: IntArray = IntArray(numV) { u ->
        if (u < lSize) 0
        else graph.multiplicities[u]
    }

    private var epsilon: LargeWeight = 0
    private val potential: LargeWeightArray = LargeWeightArray(numV)
    private val excess: IntArray = IntArray(numV)
    private val used: IntArray = IntArray(numE)
    private val currentEdge: IntArray = IntArray(numV + 1)

    companion object {
        fun run(params: Params, graph: BipartiteGraph) {
            CostScaling(params, graph).run()
        }
    }

    data class Params(
        val scalingFactor: Weight = 2
    ) {
        init {
            check(scalingFactor > 1) { "Scaling factor should be greater than 1 but was $scalingFactor" }
        }
    }

    private fun run() {
        check(lSize > 0 && rSize > 0)

        epsilon = weights.max() ?: 1
        potential.fill(0)
        start()
        val pot = potential.map { (it.toDouble() / initialScale).roundToInt() }
        println(pot)
        for (e in 0 until numE) {
            println("${used[e]} : ${graph.weights[e] - pot[sources[e]] + pot[targets[e]]}")
        }
    }

    private fun initPhase() {
        edgeStarts.copyInto(currentEdge)
        used.fill(0, fromIndex = 0, toIndex = edgeStarts[lSize])
        used.fill(1, fromIndex = edgeStarts[lSize], toIndex = numE)
        excess.fill(0)

        // Saturate arcs that breaks 0-optimality to make the pseudo assignment 0-optimal
        for (u in 0 until lSize) {
            val potentialU = potential[u]
            for (e in edgeStarts[u] until edgeStarts[u + 1]) {
                val v = targets[e]
                val rWeight = weights[e] - potentialU - potential[v]
                if (rWeight > 0) {
                    used[e] = 1
                    used[reverse[e]] = 0
                    --excess[u]
                    ++excess[v]
                }
            }
        }
    }

    /**
     * Negative return value means it's a deficit
     */
    private fun getExcess(u: Node): Int {
        val p = potential[u]
        return when {
            p < -epsilon -> excess[u] - maxExcess[u]
            p > epsilon -> excess[u] - minExcess[u]
            excess[u] > maxExcess[u] -> excess[u] - maxExcess[u]
            excess[u] < minExcess[u] -> excess[u] - minExcess[u]
            else -> 0
        }
    }

    private fun start() {
        do {
            epsilon = (epsilon + params.scalingFactor - 1) / params.scalingFactor
            initPhase()
            println("Epsilon: $epsilon")
            var cont = true
            while (cont) {
                cont = false
                var changed = false
                for (u in 0 until numV) {
                    var currentExcess = getExcess(u)
                    if (currentExcess != 0) cont = true
                    if (currentExcess <= 0) continue
                    val potentialU = potential[u]
                    for (e in edgeStarts[u] until edgeStarts[u + 1]) {
                        if (used[e] == 1) continue
                        val v = targets[e]
                        val rWeight = weights[e] - potentialU + potential[v]
                        if (rWeight <= 0) continue
                        val re = reverse[e]
                        used[e] = 1
                        used[re] = 0
                        --this.excess[u]
                        ++this.excess[v]
                        --currentExcess
                        if (currentExcess == 0) break
                    }
                    if (currentExcess > 0) potential[u] -= epsilon
                    changed = true
                }
                if (cont && !changed) {
                    for (u in 0 until numV) {
                        potential[u] += epsilon
                    }
                }
                println(potential.joinToString(separator = ", "))
            }
        } while (epsilon > 1)
    }
}
