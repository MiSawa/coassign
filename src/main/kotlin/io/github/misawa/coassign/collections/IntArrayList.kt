package io.github.misawa.coassign.collections

import it.unimi.dsi.fastutil.ints.IntList
import it.unimi.dsi.fastutil.ints.IntListIterator
import java.util.Arrays
import java.util.Spliterator
import java.util.stream.IntStream
import java.util.stream.StreamSupport


class IntArrayList private constructor(private val inner: it.unimi.dsi.fastutil.ints.IntArrayList) {
    val isEmpty: Boolean get() = inner.isEmpty
    val size: Int get() = inner.size

    constructor() : this(it.unimi.dsi.fastutil.ints.IntArrayList())
    constructor(size: Int) : this(it.unimi.dsi.fastutil.ints.IntArrayList(size))
    constructor(size: Int, init: (Int) -> Int) : this(it.unimi.dsi.fastutil.ints.IntArrayList(size)) {
        for (i in 0 until size) inner.add(init(i))
    }

    fun add(element: Int): Boolean = inner.add(element)
    operator fun get(index: Int): Int = inner.getInt(index)
    operator fun set(index: Int, element: Int): Int = inner.set(index, element)
    fun removeAt(index: Int): Int = inner.removeInt(index)
    fun clear(): Unit = inner.clear()
    fun isNotEmpty(): Boolean = inner.isNotEmpty()
    //    operator fun iterator(): PrimitiveIterator.OfInt = object : PrimitiveIterator.OfInt by inner.iterator() {}
    operator fun iterator(): IntIterator = object : IntIterator() {
        val innerIter: IntListIterator = inner.iterator()
        override fun hasNext(): Boolean = innerIter.hasNext()
        override fun nextInt(): Int = innerIter.nextInt()
    }

    fun listIterator(): IntListIterator = object : IntListIterator by inner.listIterator() {}
    fun listIterator(index: Int): IntListIterator = object : IntListIterator by inner.listIterator(index) {}
    fun subList(fromIndex: Int, toIndex: Int): IntList = inner.subList(fromIndex, toIndex)

    fun spliterator(): Spliterator.OfInt = Arrays.spliterator(inner.elements(), 0, inner.size)
    fun parallelIntStream(): IntStream = StreamSupport.intStream(spliterator(), true)
    fun intStream(): IntStream = Arrays.stream(inner.elements()).limit(inner.size.toLong())

    fun push(element: Int) = inner.add(element)
    fun top(): Int = inner.topInt()
    fun pop(): Int = inner.popInt()

    fun setSize(len: Int) = inner.size(len)
}