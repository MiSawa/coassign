package io.github.misawa.coassign

import kotlin.math.max
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
    private val multiplicities: IntArray = graph.multiplicities

    private var epsilon: LargeWeight = 0
    private val potential: LargeWeightArray = LargeWeightArray(numV)
    private val numMatch: IntArray = IntArray(numV)
    private val inMatch: IntArray = IntArray(numE)
    //    private val residualEdge = Bitset(numE)
    private val currentEdge: IntArray = IntArray(numV + 1)

    companion object {
        fun run(params: Params, graph: BipartiteGraph) {
            CostScaling(params, graph).run()
        }
    }

    data class Params(
        val scalingFactor: Weight = 10,
        val maxPartialPathLength: Int = 4,
        val priceRefinementLimit: Int = 2,
        val globalUpdateFactor: Double = 1.0
    )

    private fun run() {
        check(lSize > 0 && rSize > 0)

        epsilon = weights.max() ?: 1
        potential.fill(0)
//        residualEdge.setRange(0, edgeStarts[lSize])
        start()
        val pot = potential.map { (it.toDouble() / initialScale).roundToInt() }
        println(pot)
        for (e in 0 until numE) {
            println("${inMatch[e]} : ${graph.weights[e] - pot[sources[e]] - pot[targets[e]]}")
        }
    }

    /*
    private inline fun iterateResidualEdge(u: Node, handler: (Edge) -> Unit) {
        val edgeEnd = edgeStarts[u + 1]
        var e = residualEdge.firstSetBitInRange(edgeStarts[u], edgeEnd)
        while (e != -1) {
            handler.invoke(e)
            e = residualEdge.firstSetBitInRange(e + 1, edgeEnd)
        }
    }
    */

    private fun initPhase() {
        edgeStarts.copyInto(currentEdge)
        numMatch.fill(0)
        inMatch.fill(0)

        // Saturate arcs that breaks 0-optimality to make the pseudo assignment 0-optimal
        for (u in 0 until numV) {
            val potentialU = potential[u];
            for (e in edgeStarts[u] until edgeStarts[u + 1]) {
                val v = targets[e]
                val rWeight = weights[e] - potentialU - potential[v]
                if (rWeight > 0) {
                    inMatch[e] = 1
                    ++numMatch[v]
                }
            }
        }
    }

    /**
     * Negative return value means it's a deficit
     */
    private fun getExcess(u: Node): Int {
        return if (potential[u] < -epsilon) { // target numMatch is 0
            numMatch[u]
        } else if (potential[u] > epsilon) { // target numMatch is the multiplicity
            numMatch[u] - multiplicities[u]
        } else { // target numMatch is just a nearest feasible one
            max(0, numMatch[u] - multiplicities[u]) // excess or nothing
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
                    var excess = getExcess(u)
                    if (excess == 0) continue
                    cont = true
                    val potentialU = potential[u]
                    if (u < lSize && excess < 0){
                        for (e in edgeStarts[u] until edgeStarts[u + 1]) {
                            if (inMatch[e] == 1) continue
                            val v = targets[e]
                            val rWeight = weights[e] - potentialU - potential[v]
                            if (rWeight <= 0) continue
                            val re = reverse[e]
                            inMatch[e] = 1
                            inMatch[re] = 1
                            ++numMatch[u]
                            ++numMatch[v]
                            ++excess
                            if (excess == 0) break
                        }
                        if (excess < 0) potential[u] -= epsilon
                        changed = true
                    } else if (u >= lSize && excess > 0) {
                        for (e in edgeStarts[u] until edgeStarts[u + 1]) {
                            if (inMatch[e] == 0) continue
                            val v = targets[e]
                            val rWeight = weights[e] - potentialU - potential[v]
                            if (rWeight >= 0) continue
                            val re = reverse[e]
                            inMatch[e] = 0
                            inMatch[re] = 0
                            --numMatch[u]
                            --numMatch[v]
                            --excess
                            if (excess == 0) break
                        }
                        if (excess > 0) potential[u] += epsilon
                        changed = true
                    }
                }
                if (cont && !changed) {
                    for (u in 0 until numV) {
                        if (u < lSize) {
                            potential[u] += epsilon
                        } else {
                            potential[u] -= epsilon
                        }
                    }
                }
                println(potential.joinToString(separator = ", "))
            }
        } while (epsilon > 1)
    }
}
