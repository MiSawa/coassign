package io.github.misawa.coassign

import io.github.misawa.coassign.collections.BucketsLL
import io.github.misawa.coassign.collections.FixedCapacityIntArrayList
import io.github.misawa.coassign.utils.StatsTracker
import mu.KLogging
import kotlin.math.min

class WeightScalingDinitz(
    private val graph: BipartiteGraph,
    private val params: Params,
    statsTracker: StatsTracker
) {
    private val globalRelabelStats = statsTracker["GlobalRelabel"]
    private val dfsStats = statsTracker["DFS"]

    private val initialScale: LargeWeight

    private val rNumV: Int
    private val root: Node
    private val edgeStarts: IntArray
    private val sources: IntArray
    private val targets: IntArray
    private val reverse: IntArray
    private val weights: LargeWeightArray

    private val allNodes: NodeRange
    private val nonRootNodes: NodeRange
    private val leftNodes: NodeRange
    private val rightNodes: NodeRange
    private val allEdges: EdgeRange

    private val maxRank: Int
    private var epsilon: LargeWeight
    private val potential: LargeWeightArray
    private val currentEdge: IntArray
    private val isResidual: BooleanArray
    private val excess: FlowArray
    private val buckets: BucketsLL
    private val deficitNodes: FixedCapacityIntArrayList
    private val capToRoot: FlowArray
    private val capFromRoot: FlowArray
    private val rcapToRoot: FlowArray
    private val rcapFromRoot: FlowArray

    companion object : KLogging() {
        fun run(
            graph: BipartiteGraph,
            params: Params = Params(),
            statsTracker: StatsTracker = StatsTracker.noop()
        ): Solution = WeightScalingDinitz(graph, params, statsTracker).run()
    }

    data class Params(
        val scalingFactor: Weight = 2,
        val checkIntermediateStatus: Boolean = false
    ) {
        init {
            check(scalingFactor > 1) { "Scaling factor should be greater than 1 but was $scalingFactor" }
        }
    }

    init {
        initialScale = (graph.lSize + 2).toLargeWeight() * params.scalingFactor
        leftNodes = 0 until graph.lSize
        rightNodes = graph.lSize until graph.numV
        rNumV = graph.numV + 1
        root = graph.numV
        allNodes = 0..graph.numV
        nonRootNodes = 0 until graph.numV

        val originalNodes = 0 until graph.numV
        val rNumE = graph.numE + graph.numV * 2
        allEdges = 0 until rNumE
        edgeStarts = IntArray(graph.numV + 2)
        sources = IntArray(rNumE)
        targets = IntArray(rNumE)
        reverse = IntArray(rNumE)
        weights = LargeWeightArray(rNumE)
        for (u in originalNodes) {
            edgeStarts[u] = graph.edgeStarts[u] + u
            graph.sources.copyInto(
                destination = sources,
                destinationOffset = edgeStarts[u],
                startIndex = graph.edgeStarts[u],
                endIndex = graph.edgeStarts[u + 1]
            )
            graph.targets.copyInto(
                destination = targets,
                destinationOffset = edgeStarts[u],
                startIndex = graph.edgeStarts[u],
                endIndex = graph.edgeStarts[u + 1]
            )
            var newE: Edge = edgeStarts[u]
            for (originalE in graph.edgeStarts[u] until graph.edgeStarts[u + 1]) {
                reverse[newE] = graph.reverse[originalE] + graph.targets[originalE]
                weights[newE] = graph.weights[originalE].toLargeWeight() * initialScale
                ++newE
            }
        }
        edgeStarts[root] = graph.edgeStarts[graph.numV] + graph.numV
        edgeStarts[root + 1] = edgeStarts[root] + graph.numV
        for (u in originalNodes) {
            val e = edgeStarts[u + 1] - 1
            val re = edgeStarts[root] + u
            sources[e] = u
            targets[e] = root
            reverse[e] = re
            sources[re] = root
            targets[re] = u
            reverse[re] = e
        }
        epsilon = weights.max() ?: initialScale
        isResidual = BooleanArray(rNumE)
        excess = FlowArray(rNumV)
        potential = LargeWeightArray(rNumV)
        currentEdge = IntArray(rNumV + 1)
        maxRank = (1 + params.scalingFactor) * (2 * graph.lSize + 2)
        logger.info { "max rank = $maxRank" }
        buckets = BucketsLL(maxRank, rNumV)
        deficitNodes = FixedCapacityIntArrayList(rNumV)
        capFromRoot = FlowArray(graph.numV) { if (it < graph.lSize) graph.multiplicities[it] else 0 }
        capToRoot = FlowArray(graph.numV) { if (it >= graph.lSize) graph.multiplicities[it] else 0 }
        rcapFromRoot = capFromRoot.clone()
        rcapToRoot = capToRoot.clone()
    }

    private fun run(): Solution {
        check(graph.lSize > 0 && graph.rSize > 0)

        epsilon = weights.max() ?: 1
        potential.fill(0)
        start()

        for (u in allNodes) potential[u] -= potential[root]
        for (u in allNodes) potential[u] =
            (potential[u].toReal() / initialScale.toReal()).roundToLargeWeight() * initialScale
        logger.info { "Tighten potential" }
        // Tighten dual variables using Bellman-Ford method
        run {
            var cnt = 0
            var cont = true
            while (cont) {
                check(cnt++ < rNumV)
                cont = false
                for (e in allEdges) {
                    if (isResidual[e]) {
                        val u = sources[e]
                        val v = targets[e]
                        val tmp = potential[u] - weights[e]
                        if (tmp < potential[v]) {
                            potential[v] = tmp
                            cont = true
                        }
                    }
                }
            }
        }
        // Adjust offset
        for (u in allNodes) potential[u] -= potential[root]
        val used = BooleanArray(graph.numE)
        for (u in nonRootNodes) for (e in edgeStarts[u] until (edgeStarts[u + 1] - 1)) used[e - u] = !isResidual[e]
        val pot = WeightArray(graph.numV) {
            (potential[it].toReal() / initialScale.toReal()).roundToLargeWeight().toWeight()
        }
        logger.info { "Finish everything" }
        return Solution(graph, used, pot)
    }

    private fun checkInvariants() {
        if (!params.checkIntermediateStatus) return
        for (e in allEdges) {
            val u = sources[e]
            val v = targets[e]
            val rWeight = weights[e] - potential[u] + potential[v]
            val re = reverse[e]
            if (isResidual[re]) check(rWeight >= -epsilon)
            if (u == root) {
                check((rcapToRoot[v] > 0) == isResidual[re])
                check((rcapFromRoot[v] > 0) == isResidual[e])
            } else if (v == root) {
                check((rcapToRoot[u] > 0) == isResidual[e])
                check((rcapFromRoot[u] > 0) == isResidual[re])
            } else {
                check(isResidual[e] xor isResidual[re])
            }
        }
        val ex = FlowArray(rNumV)
        for (u in leftNodes) for (e in edgeStarts[u] until edgeStarts[u + 1] - 1) if (!isResidual[e]) {
            --ex[u]
            ++ex[targets[e]]
        }
        for (u in nonRootNodes) {
            val flowFromRoot = capFromRoot[u] - rcapFromRoot[u]
            ex[u] += flowFromRoot
            ex[root] -= flowFromRoot
        }
        check(ex.contentEquals(excess))
    }

    private fun initPhase() {
        edgeStarts.copyInto(currentEdge)
        isResidual.fill(true, fromIndex = leftNodes.first, toIndex = leftNodes.last + 1)
        isResidual.fill(false, fromIndex = leftNodes.last, toIndex = isResidual.size)
        excess.fill(0)

        // Saturate arcs that breaks 0-optimality to make the pseudo assignment 0-optimal
        for (u in leftNodes) {
            val potentialU = potential[u]
            for (e in edgeStarts[u] until edgeStarts[u + 1] - 1) {
                val v = targets[e]
                val rWeight = weights[e] - potentialU + potential[v]
                if (rWeight > 0) {
                    isResidual[e] = false
                    isResidual[reverse[e]] = true
                    --excess[u]
                    ++excess[v]
                } else {
                    isResidual[e] = true
                    isResidual[reverse[e]] = false
                }
            }
        }
        for (e in edgeStarts[root] until edgeStarts[root + 1]) {
            val v = targets[e]
            val rWeight = potential[v] - potential[root]
            val flowFromRoot = if (rWeight > 0) capFromRoot[v] else -capToRoot[v]
            excess[root] -= flowFromRoot
            excess[v] += flowFromRoot
            rcapFromRoot[v] = capFromRoot[v] - flowFromRoot
            rcapToRoot[v] = capToRoot[v] + flowFromRoot
            isResidual[e] = rcapFromRoot[v] > 0
            isResidual[reverse[e]] = rcapToRoot[v] > 0
        }
        checkInvariants()
    }

    private fun tsort(result: FixedCapacityIntArrayList, deg: NodeToIntArray): Boolean {
        result.clear()
        deg.fill(0)
        for (u in allNodes) {
            val potentialU = potential[u]
            for (e in edgeStarts[u] until edgeStarts[u+1]) {
                if (!isResidual[e]) continue
                val v = targets[e]
                val rWeight = weights[e] - potentialU + potential[v]
                if (rWeight > 0) ++deg[v]
            }
        }
        for (u in allNodes) if (deg[u] == 0) result.push(u)
        for (u in result) {
            val potentialU = potential[u]
            for (e in edgeStarts[u] until edgeStarts[u+1]) {
                if (!isResidual[e]) continue
                val v = targets[e]
                val rWeight = weights[e] - potentialU + potential[v]
                if (rWeight > 0) if (--deg[v] == 0) result.push(v)
            }
        }
        return result.size == rNumV
    }

    private fun priceRefine(): Boolean {
        val buf = NodeToIntArray(rNumV)
        val tsorted = FixedCapacityIntArrayList(rNumV)
        buckets.clear()
        while (tsort(tsorted, buf)) {
            for (u in tsorted) {
                val potentialU = potential[u]
                val label = buckets.getBucket(u)
                for (e in edgeStarts[u] until edgeStarts[u+1]) {
                    if (!isResidual[e]) continue
                    val v = targets[e]
                    val rWeight = weights[e] - potentialU + potential[v]
                    if (rWeight <= 0) continue
                }
            }
        }
        TODO()
    }

    private fun adjustPotential(): Boolean {
        globalRelabelStats.start().use {
            checkInvariants()
            edgeStarts.copyInto(currentEdge)
            deficitNodes.clear()
            buckets.clear(buckets.maxBucketId)
            var totalExcess = 0
            for (u in allNodes) if (excess[u] > 0) {
                totalExcess += excess[u]
                buckets.setBucket(0, u)
            }
            if (totalExcess == 0) return false
            var d: Int = 0
            while (d <= buckets.maxBucketId) {
                while (buckets.hasNextElement(d)) {
                    val u = buckets.nextElement(d)
                    if (excess[u] < 0) {
                        deficitNodes.push(u)
                        totalExcess += excess[u]
                        if (totalExcess == 0) break
                    }
                    val potentialU = potential[u]
                    for (e in edgeStarts[u] until edgeStarts[u + 1]) {
                        if (!isResidual[e]) continue
                        val v = targets[e]
                        val currentDistV = buckets.getBucket(v).coerceAtMost(maxRank)
                        if (d >= currentDistV) continue
                        val rWeight = weights[e] - potentialU + potential[v]
                        val diff: Int = if (rWeight > 0) {
                            0
                        } else {
                            (((-rWeight) / epsilon) + 1).coerceAtMost(maxRank.toLong()).toInt()
                        }
                        if (d + diff < currentDistV) {
                            buckets.setBucket(d + diff, v)
                        }
                    }
                }
                if (totalExcess == 0) break
                ++d
            }
            for (u in allNodes) {
                potential[u] += min(d, buckets.getBucket(u)) * epsilon
            }
            check(deficitNodes.isNotEmpty())
            checkInvariants()
            return true
        }
    }

    private fun start() {
        initPhase()
        val path = FixedCapacityIntArrayList(rNumV * 2)
        val inPath = BooleanArray(rNumV)
        do {
            checkInvariants()
            epsilon = ((epsilon + params.scalingFactor - 1) / params.scalingFactor).coerceAtLeast(1)
            logger.trace { "Phase $epsilon" }
            initPhase()
            checkInvariants()
            while (adjustPotential()) {
                // blocking flow
                while (deficitNodes.isNotEmpty()) {
                    val deficitNode = deficitNodes.top()
                    if (excess[deficitNode] >= 0 || currentEdge[deficitNode] == edgeStarts[deficitNode+1]) {
                        deficitNodes.pop()
                        continue
                    }
                    run {
                        var i = 1
                        while (i < path.size) {
                            inPath[path[i]] = false
                            i += 2
                        }
                    }
                    path.clear()
                    path.push(-1)
                    path.push(deficitNode)
                    inPath[deficitNode] = true
                    dfsStats.start()
                    run findPath@{
                        while (path.isNotEmpty()) {
                            val u = path.top()
                            if (excess[u] > 0) break
                            val potentialU = potential[u]
                            run onPathTip@{
                                var e = currentEdge[u]
                                while (e < edgeStarts[u + 1]) {
                                    val done = run onEdge@{
                                        val v = targets[e]
                                        val rWeight = weights[e] - potentialU + potential[v]
                                        if (rWeight >= 0) return@onEdge false
                                        val re = reverse[e]
                                        if (!isResidual[re]) return@onEdge false
                                        if (currentEdge[v] == edgeStarts[v+1]) return@onEdge false
                                        path.push(e)
                                        path.push(v)
                                        if (inPath[v]) return@findPath
                                        inPath[v] = true
                                        return@onEdge true
                                    }
                                    if (done) {
                                        currentEdge[u] = e
                                        return@onPathTip
                                    }
                                    ++e
                                }
                                currentEdge[u] = edgeStarts[u+1]
                                potential[u] += epsilon
                                checkInvariants()
                                inPath[path.pop()] = false
                                path.pop()
                                if (path.isNotEmpty()) ++currentEdge[path.top()]
                            }
                        }
                    }
                    dfsStats.stop()
                    if (path.isEmpty) {
                        deficitNodes.pop()
                        continue
                    }
                    if (path.size == 4 && ((path[1] == root) || (path[3] == root))) {
                        val flowFromRoot = path[3] == root
                        val eFromRoot = if (flowFromRoot) reverse[path[2]] else path[2]
                        val eToRoot = if (!flowFromRoot) reverse[path[2]] else path[2]
                        val other = if (flowFromRoot) path[1] else path[3]
                        val flow = if (flowFromRoot) {
                            min(excess[root], min(rcapFromRoot[other], -excess[other]))
                        } else {
                            -min(excess[other], min(rcapToRoot[other], -excess[root]))
                        }
                        excess[root] -= flow
                        excess[other] += flow
                        rcapFromRoot[other] -= flow
                        rcapToRoot[other] += flow
                        isResidual[eFromRoot] = rcapFromRoot[other] > 0
                        isResidual[eToRoot] = rcapToRoot[other] > 0
                    } else {
                        ++excess[deficitNode]
                        --excess[path.top()]
                        var i = 2
                        while (i < path.size) {
                            val u = path[i - 1]
                            val e = path[i]
                            val v = path[i + 1]
                            val re = reverse[e]
                            if (u == root) {
                                ++rcapFromRoot[v]
                                --rcapToRoot[v]
                                isResidual[e] = true
                                if (rcapToRoot[v] == 0) isResidual[re] = false
                            } else if (v == root) {
                                --rcapFromRoot[u]
                                ++rcapToRoot[u]
                                isResidual[e] = true
                                if (rcapFromRoot[u] == 0) isResidual[re] = false
                            } else {
                                isResidual[e] = true
                                isResidual[re] = false
                            }
                            i += 2
                        }
                    }

                    checkInvariants()
                }
            }
        } while (epsilon > 1)
    }
}
