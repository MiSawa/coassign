package io.github.misawa.coassign

fun main(args: Array<String>) {
    val graph = BipartiteGraph.builder(3, 2)
        .addEdge(0, 0, 3)
        .addEdge(0, 1, 1)
        .addEdge(1, 0, 3)
        .addEdge(1, 1, 2)
        .addEdge(2, 0, 2)
        .addEdge(2, 1, 1)
        .setMultiplicityLeft(0, 2)
        .setMultiplicityLeft(1, 1)
        .setMultiplicityLeft(2, 2)
        .setMultiplicityRight(0, 2)
        .setMultiplicityRight(1, 1)
        .build()
    WeightScalingPR.run(params = WeightScalingPR.Params(), graph = graph)
}