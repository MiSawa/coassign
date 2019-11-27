package io.github.misawa.coassign.collections

class DialPQLL(maxLength: Int, size: Int) {
    private val bucketEnd = size
    private val distances: IntArray = IntArray(size) { Int.MAX_VALUE }
    private val first: IntArray = IntArray(maxLength + 1) { -1 }
    private val prev: IntArray = IntArray(size + 1) { -1 }
    private val next: IntArray = IntArray(size + 1) { -1 }
    private var currentPriority: Int = 0

    fun clear() {
        currentPriority = 0
        distances.fill(Int.MAX_VALUE)
        first.fill(bucketEnd)
        prev.fill(bucketEnd)
        next.fill(bucketEnd)
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
            if (first[currentPriority] != bucketEnd) return true
            ++currentPriority
        }
        return false
    }

    fun push(distance: Int, node: Int) {
        val currentDist = distances[node]
        if (currentDist <= distance) return
        check(currentPriority <= distance)
        if (currentDist != Int.MAX_VALUE && first[currentDist] == node) {
            first[currentDist] = next[node]
        } else {
            val nx = next[node]
            val pr = prev[node]
            prev[nx] = pr
            next[pr] = nx
        }
        distances[node] = distance
        val a = first[distance]
        prev[a] = node
        next[node] = a
        first[distance] = node
    }
}