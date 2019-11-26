package io.github.misawa.coassign.collections

class DialPQ(maxLength: Int, private val size: Int) {
    private val distances: IntArray = IntArray(size) { Int.MAX_VALUE }
    private val elements: Array<IntArray> = Array(maxLength + 1) { IntArray(size) }
    private val sizes: IntArray = IntArray(maxLength + 1)

    private var currentPriority: Int = 0
    private var currentPos: Int = 0

    fun clear() {
        distances.fill(Int.MAX_VALUE)
        sizes.fill(0)
        currentPriority = 0
        currentPos = 0
    }

    fun getDist(u: Int) = distances[u]

    /**
     * Can only be called after calling [hasNextElement]
     */
    fun nextElement(): Int = elements[currentPos][--sizes[currentPos]]

    /**
     * Only valid after calling [nextElement] and before calling [hasNextElement]
     */
    fun currentPriority(): Int = currentPriority

    fun hasNextElement(): Boolean {
        for (ignored in elements.indices) {
            var i = sizes[currentPos] - 1
            while (i >= 0 && distances[elements[currentPos][i]] != currentPriority) --i
            sizes[currentPos] = i + 1
            if (i >= 0) return true
            ++currentPriority
            ++currentPos
            if (currentPos == elements.size) currentPos = 0
        }
        return false
    }

    fun push(distance: Int, node: Int) {
        if (distances[node] <= distance) return
        check(distance - currentPriority in sizes.indices)
        distances[node] = distance
        val pos = distance % elements.size
        elements[pos][sizes[pos]++] = node
    }
}