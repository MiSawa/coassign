package io.github.misawa.coassign.collections

class DialPQLL(maxLength: Int, size: Int) {
    private val distances: IntArray = IntArray(size) { Int.MAX_VALUE }
    private val first: IntArray = IntArray(maxLength + 1) { -1 }
    private val prev: IntArray = IntArray(size) { -1 }
    private val next: IntArray = IntArray(size) { -1 }
    private var currentPriority: Int = 0

    fun clear() {
        currentPriority = 0
        distances.fill(Int.MAX_VALUE)
        first.fill(-1)
        prev.fill(-1)
        next.fill(-1)
    }

    fun getDist(u: Int) = distances[u]

    /**
     * Can only be called after calling [hasNextElement]
     */
    fun nextElement(): Int {
        val ret = first[currentPriority]
        first[currentPriority] = next[ret]
        return ret
    }

    /**
     * Only valid after calling [nextElement] and before calling [hasNextElement]
     */
    fun currentPriority(): Int = currentPriority

    fun hasNextElement(): Boolean {
        while (currentPriority < first.size) {
            if (first[currentPriority] >= 0) return true
            ++currentPriority
        }
        return false
    }

    fun push(distance: Int, node: Int) {
        if (distances[node] <= distance) return
        check(currentPriority <= distance)
        if (next[node] != -1) prev[next[node]] = prev[node]
        if (prev[node] != -1) next[prev[node]] = next[node]
        distances[node] = distance
        val a = first[distance]
        prev[a] = node
        next[node] = a
        first[distance] = node
    }
}