# coassign
[![Release](https://jitpack.io/v/MiSawa/coassign.svg)](https://jitpack.io/#MiSawa/coassign)

Solves maximum-weight bipartite b-matching problem using cost-scaling method.

For a bipartite graph ![bipartite](https://latex.codecogs.com/gif.latex?%5Cinline%20G%3D%28U%20%5Csqcup%20V%2C%20E%29), weight ![weight](https://latex.codecogs.com/gif.latex?%5Cinline%20w%3A%20E%20%5Cto%20%5Cmathbb%7BZ%7D_%7B%3E%200%7D) and multiplicity ![multiplicity](https://latex.codecogs.com/gif.latex?%5Cinline%20b%20%3A%20U%20%5Csqcup%20V%20%5Cto%20%5Cmathbb%7BZ%7D_%7B%3E0%7D), maximum-weight bipartite b-matching problem is defined as follows;   

![problem](https://latex.codecogs.com/gif.latex?%5Cbegin%7Bmatrix%7D%20%5Cmax.%20%26%5Csum_%7Be%20%5Cin%20E%7D%20w_e%20x_e%20%5C%5C%20%5Cmathrm%7Bs.t.%7D%20%26%20%5Csum_%7Bu%20%5Cin%20U%7D%20x_%7Bu%2C%20v%7D%20%5Cle%20b_v%20%5C%5C%20%26%20%5Csum_%7Bv%20%5Cin%20V%7D%20x_%7Bu%2C%20v%7D%20%5Cle%20b_u%20%5C%5C%20%26%20x_e%20%5Cin%20%5C%7B0%2C%201%5C%7D%20%5Cend%7Bmatrix%7D)

This library is to solve the above problem using cost-scaling algorithm.


### Notes
- If C++ is acceptable in your situation, I _highly_ recommend you to try Minimum Cost Flow algorithm implementations of [lemon](https://lemon.cs.elte.hu/trac/lemon).
- If you want to use this in production, you'd probably better to have a fallback method, like greedy algorithm for example.
- API is subject to change. Don't trust me.
- Please [create an issue](https://github.com/MiSawa/coassign/issues/new) when you see a weird behavior. An instance or a code to reproduce the behavior is appreciated. Without that... it's gonna be hard to debug. 


### Usage

First, you need to declare this library as a dependency.

This library is published to [jitpack](https://jitpack.io/).
If you're using gradle, then the lines you will want to add to `build.gradle` would be something like
```groovy
repositories {
  jcenter()
  maven { url 'https://jitpack.io' }
}
dependencies {
  implementation 'com.github.MiSawa:coassign:commit-hash-here'
}
```

Please note that you should use commit hash to fix the version you use, as API is subject to change.

Then, you can use `io.github.misawa.coassign.BipartiteGraph` to define a bipartite graph,
and `io.github.misawa.coassign.WeightScaling` to solve the problem.
```kotlin
fun main() {
    // These builder methods have some check on their arguments
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

    // May throw an exception if there's a bug.
    val result = WeightScaling.run(
        params = WeightScaling.Params(),
        graph = graph
    )

    // This will make sure the solution we have is feasible (using primal variables) and optimal (using dual variables).
    result.check()

    println(result.getMatches()) // [(1, 0), (0, 0), (0, 1)]
    println(result.value) // 7
}
```
