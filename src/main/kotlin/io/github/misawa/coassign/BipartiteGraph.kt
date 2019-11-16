package io.github.misawa.coassign

import io.github.misawa.coassign.collections.IntArrayList

class BipartiteGraph(builder: Builder) {
    companion object {
        fun builder(lSize: Int, rSize: Int): Builder = Builder(lSize, rSize)
    }

    internal val numV: Int
    internal val numE: Int
    internal val lSize: Int
    internal val rSize: Int
    internal val flipped: Boolean
    internal val edgeStarts: IntArray
    internal val sources: IntArray
    internal val targets: IntArray
    internal val reverse: IntArray
    internal val weights: WeightArray
    internal val multiplicities: FlowArray

    init {
        lSize = builder.lSize
        rSize = builder.rSize
        flipped = builder.flipped
        numV = lSize + rSize
        val originalNumE = builder.weights.size
        numE = originalNumE * 2

        edgeStarts = IntArray(numV + 1)
        for (l in builder.lefts) ++edgeStarts[l]
        for (r in builder.rights) ++edgeStarts[r + lSize]
        for (i in 0 until numV) edgeStarts[i + 1] += edgeStarts[i]
        sources = IntArray(numE)
        targets = IntArray(numE)
        reverse = IntArray(numE)
        weights = IntArray(numE)
        for (e in 0 until originalNumE) {
            val u = builder.lefts[e]
            val v = builder.rights[e] + lSize
            val forward = --edgeStarts[u]
            val backward = --edgeStarts[v]
            sources[forward] = u
            targets[forward] = v
            sources[backward] = v
            targets[backward] = u
            reverse[forward] = backward
            reverse[backward] = forward
            weights[forward] = builder.weights[e]
            weights[backward] = -builder.weights[e]
        }
        multiplicities = FlowArray(numV) {
            if (it < lSize) builder.multiplicityL[it]
            else builder.multiplicityR[it - lSize]
        }
    }

    class Builder internal constructor(
        internal val lSize: Int,
        internal val rSize: Int,
        internal val flipped: Boolean = false,
        internal val lefts: IntArrayList = IntArrayList(),
        internal val rights: IntArrayList = IntArrayList(),
        internal val weights: WeightArrayList = WeightArrayList(),
        internal val multiplicityL: FlowArrayList = FlowArrayList(lSize) { 0 },
        internal val multiplicityR: FlowArrayList = FlowArrayList(rSize) { 0 }
    ) {
        private fun checkL(leftId: Int) =
            leftId.also { check(leftId in 0 until lSize) { "Left node id $leftId should be in range 0..$lSize" } }

        private fun checkR(rightId: Int) =
            rightId.also { check(rightId in 0 until rSize) { "Right node id $rightId should be in range 0..$rSize" } }

        fun setMultiplicityLeft(leftId: Int, multiplicity: Flow): Builder = this.also {
            multiplicityL[checkL(leftId)] = multiplicity
        }

        fun setMultiplicityRight(rightId: Int, multiplicity: Flow): Builder = this.also {
            multiplicityR[checkR(rightId)] = multiplicity
        }

        fun addEdge(leftId: Int, rightId: Int, weight: Weight): Builder = this.also {
            check(weight >= 0) { "Weight should be non-negative but was $weight" }
            lefts.add(checkL(leftId))
            rights.add(checkR(rightId))
            weights.add(weight)
        }

        private fun flipped() = Builder(rSize, lSize, !flipped, rights, lefts, weights, multiplicityR, multiplicityL)

        fun build(): BipartiteGraph = if (lSize < rSize) {
            BipartiteGraph(this)
        } else {
            BipartiteGraph(flipped())
        }
    }
}