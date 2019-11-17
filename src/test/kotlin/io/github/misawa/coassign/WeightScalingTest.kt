package io.github.misawa.coassign

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.random.Random

internal class WeightScalingTest {
    companion object {
        private const val NUM_INSTANCES: Int = 10
        private const val SEED: Int = 42
        private val PARAMETER: RandomGraph.Parameter = RandomGraph.Parameter(
            lSize = 1 until 10,
            rSize = 1 until 10,
            lMultiplicity = 1 until 3,
            rMultiplicity = 1 until 2,
            weight = 2 until 10,
            density = 0.6
        )

        @Suppress("unused")
        @JvmStatic
        fun generateRandomBipartiteGraph(): List<BipartiteGraph> {
            val rng = Random(SEED)
            return (0 until NUM_INSTANCES).map {
                RandomGraph.generate(
                    parameter = PARAMETER,
                    seed = rng.nextLong()
                )
            }
        }
    }

    @ParameterizedTest
    @MethodSource("generateRandomBipartiteGraph")
    fun testRandomly(graph: BipartiteGraph) {
        WeightScaling.run(graph)
    }
}