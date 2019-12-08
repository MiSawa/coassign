package io.github.misawa.coassign.collections

interface ActiveNodeQueue {
    fun push(node: Int)
    fun pop(): Int
    fun isEmpty(): Boolean
    fun isNotEmpty(): Boolean
    fun clear()
}

class FIFOActiveNodeQueue(size: Int) : ActiveNodeQueue{
    private val size = size + 1
    private var version: Byte = 1
    private val inQueue = ByteArray(this.size)
    private val queue = IntArray(this.size)
    private var l: Int = 0
    private var r: Int = 0
    override fun push(node: Int) {
        if (inQueue[node] == version) return
        inQueue[node] = version
        queue[r++] = node
        if (r == size) r = 0
    }

    override fun isEmpty(): Boolean = l == r
    override fun isNotEmpty(): Boolean = l != r
    override fun pop(): Int = queue[l++].also {
        inQueue[it] = 0
        if (l == size) l = 0
    }

    override fun clear() {
        l = 0
        r = 0
        if (++version == 0.toByte()) {
            inQueue.fill(0)
        }
    }
}