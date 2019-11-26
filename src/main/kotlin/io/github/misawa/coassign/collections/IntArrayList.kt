package io.github.misawa.coassign.collections

import it.unimi.dsi.fastutil.ints.IntList
import it.unimi.dsi.fastutil.ints.IntListIterator
import java.util.Arrays
import java.util.PrimitiveIterator
import java.util.Spliterator
import java.util.stream.IntStream
import java.util.stream.StreamSupport


class IntArrayList private constructor(private val inner: it.unimi.dsi.fastutil.ints.IntArrayList) :
    MutableList<Int> by inner {
    constructor() : this(it.unimi.dsi.fastutil.ints.IntArrayList())
    constructor(size: Int) : this(it.unimi.dsi.fastutil.ints.IntArrayList(size))
    constructor(size: Int, init: (Int) -> Int) : this(it.unimi.dsi.fastutil.ints.IntArrayList(size)) {
        for (i in 0 until size) inner.add(init(i))
    }

    override fun get(index: Int): Int = inner.getInt(index)
    override fun removeAt(index: Int): Int = inner.removeInt(index)
    override fun iterator(): PrimitiveIterator.OfInt = object : PrimitiveIterator.OfInt by inner.iterator() {}
    override fun listIterator(): IntListIterator = object : IntListIterator by inner.listIterator() {}
    override fun listIterator(index: Int): IntListIterator = object : IntListIterator by inner.listIterator(index) {}
    override fun subList(fromIndex: Int, toIndex: Int): IntList = inner.subList(fromIndex, toIndex)

    override fun spliterator(): Spliterator.OfInt = Arrays.spliterator(inner.elements(), 0, inner.size)
    fun parallelIntStream(): IntStream = StreamSupport.intStream(spliterator(), true)
    fun intStream(): IntStream = Arrays.stream(inner.elements()).limit(inner.size.toLong())

    fun push(element: Int) = add(element)
    fun top(): Int = inner.topInt()
    fun pop(): Int = inner.popInt()

    fun setSize(len: Int) = inner.size(len)
}