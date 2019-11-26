package io.github.misawa.coassign.collections

class DialPQ(maxLength: Int, size: Int) {
    private val distances: IntArray = IntArray(size) { Int.MAX_VALUE }
    private val elements: Array<IntArrayList> = Array(maxLength + 1) { IntArrayList(1) }

    private var currentPriority: Int = 0
    private var currentPos: Int = 0

    fun clear() {
        distances.fill(Int.MAX_VALUE)
        elements.forEach { it.clear() }
        currentPriority = 0
        currentPos = 0
    }

    fun getDist(u: Int) = distances[u]

    /**
     * Can only be called after calling [hasNextElement]
     */
    fun nextElement(): Int = elements[currentPos].pop()

    /**
     * Only valid after calling [nextElement] and before calling [hasNextElement]
     */
    fun currentPriority(): Int = currentPriority

    fun hasNextElement(): Boolean {
        for (ignored in elements.indices) {
            val currentElems = elements[currentPos]
            while (currentElems.isNotEmpty() && distances[currentElems.top()] != currentPriority)
                currentElems.pop()
            if (currentElems.isNotEmpty()) return true
            ++currentPriority
            ++currentPos
            if (currentPos == elements.size) currentPos = 0
        }
        return false
    }

    fun push(distance: Int, node: Int) {
        if (distances[node] <= distance) return
        val diff = distance - currentPriority
        check(diff >= 0)
        check(diff < elements.size)
        distances[node] = distance
        val pos = distance % elements.size
        elements[pos].push(node)
    }
}