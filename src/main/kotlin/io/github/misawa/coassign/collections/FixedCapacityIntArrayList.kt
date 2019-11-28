package io.github.misawa.coassign.collections

class FixedCapacityIntArrayList(maxCapacity: Int) {
    private val arr = IntArray(maxCapacity)
    private var currentSize: Int = 0

    val isEmpty: Boolean get() = currentSize == 0
    val size get() = currentSize
    fun add(element: Int) {
        arr[currentSize++] = element
    }

    operator fun get(index: Int): Int = arr[index]
    operator fun set(index: Int, element: Int) {
        arr[index] = element
    }

    fun clear(): Unit {
        currentSize = 0
    }

    fun isNotEmpty(): Boolean = currentSize > 0
    operator fun iterator(): IntIterator = object : IntIterator() {
        private var i = 0
        override fun hasNext(): Boolean = i < currentSize
        override fun nextInt(): Int = arr[i++]
    }

    fun push(element: Int) {
        arr[currentSize++] = element
    }

    fun top(): Int = arr[currentSize - 1]
    fun pop(): Int = arr[--currentSize]

    fun setSize(len: Int) {
        currentSize = len
    }
}