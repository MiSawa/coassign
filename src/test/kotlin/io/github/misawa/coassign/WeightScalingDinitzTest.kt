package io.github.misawa.coassign

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

internal class WeightScalingDinitzTest {
    companion object {
        private const val NUM_INSTANCES: Int = 10000
        private val SMALL: RandomGraph.Parameter = RandomGraph.Parameter(
            lSize = 1 until 4,
            rSize = 1 until 4,
            lMultiplicity = 1 until 5,
            rMultiplicity = 1 until 5,
            weight = 1 until 5,
            density = 0.5
        )
        private val MEDIUM: RandomGraph.Parameter = RandomGraph.Parameter(
            lSize = 1 until 50,
            rSize = 1 until 50,
            lMultiplicity = 1 until 10,
            rMultiplicity = 1 until 10,
            weight = 2 until 100,
            density = 0.5
        )

        private fun gen(param: RandomGraph.Parameter) =
            (0 until NUM_INSTANCES).map { RandomGraph.generate(param, it.toLong()) }

        @Suppress("unused")
        @JvmStatic
        fun generateRandomBipartiteGraph(): List<BipartiteGraph> {
            return listOf(
                gen(SMALL.copy(density = 0.1, lMultiplicity = 0 until 3, rMultiplicity = 0 until 3)),
                gen(SMALL.copy(density = 0.5)),
                gen(SMALL.copy(density = 1.0)),
                gen(MEDIUM.copy(density = 0.1)),
                gen(MEDIUM.copy(density = 0.5)),
                gen(MEDIUM.copy(density = 1.0))
            ).flatten()
        }
    }

    @ParameterizedTest
    @MethodSource("generateRandomBipartiteGraph")
    @Timeout(1)
    fun testRandomly(graph: BipartiteGraph) {
        val solution = WeightScalingDinitz.run(graph, params = WeightScalingDinitz.Params(checkIntermediateStatus = true))
        solution.check()
    }
}