package io.github.misawa.coassign

import mu.KLogging
import kotlin.math.max
import kotlin.math.min

class WeightScalingPR(
    private val graph: BipartiteGraph,
    private val params: Params
) {
    private val initialScale = (graph.lSize + 2) * params.scalingFactor
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

    private val nodes: NodeRange = 0 until numV
    private val leftNodes: NodeRange = 0 until lSize
    private val rightNodes: NodeRange = lSize until numV
    private val edges: EdgeRange = 0 until numE
    private val forwardEdges: EdgeRange = 0 until edgeStarts[lSize]

    private val minExcessOrig: IntArray = IntArray(numV) { u ->
        if (u < lSize) -graph.multiplicities[u]
        else 0
    }
    private val maxExcessOrig: IntArray = IntArray(numV) { u ->
        if (u < lSize) 0
        else graph.multiplicities[u]
    }
    private val minExcess: IntArray = minExcessOrig.copyOf()
    private val maxExcess: IntArray = maxExcessOrig.copyOf()

    private var epsilon: LargeWeight = 0
    private val potential: LargeWeightArray = LargeWeightArray(numV)
    private val excess: IntArray = IntArray(numV)
    private val used: BooleanArray = BooleanArray(numE)
    private val currentEdge: IntArray = IntArray(numV + 1)

    companion object : KLogging() {
        fun run(graph: BipartiteGraph, params: Params = Params()): LargeWeight = WeightScalingPR(graph, params).run()
    }

    data class Params(
        val scalingFactor: Weight = 2
    ) {
        init {
            check(scalingFactor > 1) { "Scaling factor should be greater than 1 but was $scalingFactor" }
        }
    }

    private fun sgn(u: Node): Int = if (u < lSize) 1 else -1

    private fun run(): LargeWeight {
        check(lSize > 0 && rSize > 0)

        epsilon = weights.max() ?: 1
        potential.fill(0)
        start()

        fun getDualValue(pot: WeightArray): LargeWeight {
            var dual: LargeWeight = 0
            for (u in nodes) dual += graph.multiplicities[u] * max(0, sgn(u) * pot[u]).toLargeWeight()
            for (e in forwardEdges) dual += max(0, graph.weights[e] - pot[sources[e]] + pot[targets[e]])
            return dual
        }

        fun checkDualFeasibility(pot: WeightArray) {
            for (e in edges) {
                val rWeight = graph.weights[e] - pot[sources[e]] + pot[targets[e]]
                if (rWeight > 0) check(used[e]) { "There's an edge that has positive residual weight but not used" }
                if (rWeight < 0) check(!used[e]) { "There's an edge that has negative residual weight but used" }
            }
        }

        // --- Work for primal ---
        // Check primal feasibility
        val matchCount = IntArray(numV)
        for (e in forwardEdges) if (used[e]) {
            ++matchCount[sources[e]]
            ++matchCount[targets[e]]
        }
        for (u in nodes) check(matchCount[u] <= graph.multiplicities[u]) { "The matching includes too many edges adjacent to $u" }

        // Extract primal value
        var result: LargeWeight = 0
        for (e in forwardEdges) if (used[e]) result += graph.weights[e]

        // --- Work for dual ---
        // Round potential FIXME: Don't need this.
        for (i in nodes) {
            potential[i] = (potential[i].toReal() / initialScale).roundToLargeWeight() * initialScale
        }

        // Scale-down dual to the original scale
        val pot = WeightArray(numV) { (potential[it].toDouble() / initialScale).roundToLargeWeight().toWeight() }

        // Tighten dual variables
        run {
            var offset: Weight = 0
            for (ignored in nodes) {
                for (u in nodes) {
                    if (excess[u] > minExcessOrig[u]) pot[u] = min(pot[u], offset)
                    if (excess[u] < maxExcessOrig[u]) offset = min(offset, pot[u])
                }
                for (e in edges) {
                    if (!used[e]) {
                        val u = sources[e]
                        val v = targets[e]
                        pot[v] = min(pot[v], pot[u] - graph.weights[e])
                    }
                }
            }
            for (u in nodes) pot[u] -= offset
        }
        checkDualFeasibility(pot)
        val dual = getDualValue(pot)
        check(result == dual) { "Primal and dual value should be the same. Primal: $result, dual: $dual" }
        return result
    }

    private fun initPhase() {
        edgeStarts.copyInto(currentEdge)
        used.fill(false, fromIndex = 0, toIndex = edgeStarts[lSize])
        used.fill(true, fromIndex = edgeStarts[lSize], toIndex = numE)
        excess.fill(0)

        // Saturate arcs that breaks 0-optimality to make the pseudo assignment 0-optimal
        for (u in leftNodes) {
            val potentialU = potential[u]
            for (e in edgeStarts[u] until edgeStarts[u + 1]) {
                val v = targets[e]
                val rWeight = weights[e] - potentialU + potential[v]
                if (rWeight > 0) {
                    used[e] = true
                    used[reverse[e]] = false
                    --excess[u]
                    ++excess[v]
                }
            }
        }
        for (u in nodes) {
            reCalcExcess(u)
        }
    }

    private fun reCalcExcess(u: Node, p: LargeWeight = potential[u]) {
        when {
            p < -epsilon -> {
                maxExcess[u] = maxExcessOrig[u]
                minExcess[u] = maxExcessOrig[u]
            }
            p > epsilon -> {
                maxExcess[u] = minExcessOrig[u]
                minExcess[u] = minExcessOrig[u]
            }
            else -> {
                minExcess[u] = minExcessOrig[u]
                maxExcess[u] = maxExcessOrig[u]
            }
        }
    }

    /**
     * Negative return value means it's a deficit
     */
    private fun getExcess(u: Node): Int {
        val p = potential[u]
        return when {
            p < -epsilon -> excess[u] - maxExcessOrig[u]
            p > epsilon -> excess[u] - minExcessOrig[u]
            excess[u] > maxExcessOrig[u] -> excess[u] - maxExcessOrig[u]
            excess[u] < minExcessOrig[u] -> excess[u] - minExcessOrig[u]
            else -> 0
        }
    }

    private fun relabel(u: Node) {
        potential[u] -= epsilon
        reCalcExcess(u, potential[u])
        currentEdge[u] = edgeStarts[u]
    }

    /**
     * Returns true iff the current assignment is proper w.r.t. the potential
     */
    private fun dischargeRoot(): Boolean {
        while (true) {
            var hasDeficit = false
            for (u in nodes) {
                if (excess[u] > maxExcess[u]) return false
                hasDeficit = hasDeficit || excess[u] < minExcess[u]
            }
            if (!hasDeficit) return true
            for (u in nodes) {
                potential[u] += epsilon
                reCalcExcess(u, potential[u])
            }
        }
    }

    private fun discharge1(u: Node): Boolean {
        var ex = excess[u] - maxExcess[u]
        if (ex <= 0) return false
        val p = potential[u]
        var e = currentEdge[u] - 1
        while (++e < edgeStarts[u + 1] && ex > 0) {
            if (used[e]) continue
            val v = targets[e]
            val rWeight = weights[e] - p + potential[v]
            if (rWeight <= 0) continue
            val re = reverse[e]
            used[e] = true
            used[re] = false
            --excess[u]
            ++excess[v]
            --ex
        }
        currentEdge[u] = e
        if (ex > 0) relabel(u)
        return excess[u] - maxExcess[u] > 0
    }

    private fun discharge2(u: Node): Boolean {
        var ex = excess[u] - maxExcess[u]
        if (ex <= 0) return false
        val p = potential[u]
        var e = currentEdge[u] - 1
        while (++e < edgeStarts[u + 1] && ex > 0) {
            if (used[e]) continue
            val v = targets[e]
            val rWeight = weights[e] - p + potential[v]
            if (rWeight <= 0) continue
            val re = reverse[e]
            used[e] = true
            used[re] = false
            --excess[u]
            ++excess[v]
            --ex
            if (discharge1(v) && discharge1(v)) {
                ex = excess[u] - maxExcess[u]
            }
        }
        currentEdge[u] = e
        if (ex > 0) relabel(u)
        return excess[u] - maxExcess[u] > 0
    }

    private fun start() {
        do {
            epsilon = (epsilon + params.scalingFactor - 1) / params.scalingFactor
            logger.trace { "Phase $epsilon" }
            initPhase()
            while (true) {
                if (dischargeRoot()) break
                for (u in rightNodes) while (discharge1(u));
                var cont = true
                while (cont) {
                    cont = false
                    for (u in leftNodes) {
                        cont = cont || discharge2(u)
                    }
                }
            }
        } while (epsilon > 1)
    }
}