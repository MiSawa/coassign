package io.github.misawa.coassign

import kotlin.random.Random
import kotlin.random.nextInt

class RandomGraph {
    data class Parameter(
        val lSize: IntRange,
        val rSize: IntRange,
        val lMultiplicity: IntRange,
        val rMultiplicity: IntRange,
        val weight: IntRange,
        val density: Double
    )

    companion object {
        fun generate(parameter: Parameter, seed: Long): BipartiteGraph {
            val rng = Random(seed)
            val lSize = rng.nextInt(parameter.lSize)
            val rSize = rng.nextInt(parameter.rSize)
            val builder = BipartiteGraph.builder(lSize, rSize)
            for (i in 0 until lSize) {
                builder.setMultiplicityLeft(i, rng.nextInt(parameter.lMultiplicity))
            }
            for (i in 0 until rSize) {
                builder.setMultiplicityRight(i, rng.nextInt(parameter.rMultiplicity))
            }
            for (i in 0 until lSize) {
                for (j in 0 until rSize) {
                    if (rng.nextDouble() >= parameter.density) continue
                    val weight = rng.nextInt(parameter.weight)
                    builder.addEdge(i, j, weight)
                }
            }
            return builder.build()
        }
    }
}