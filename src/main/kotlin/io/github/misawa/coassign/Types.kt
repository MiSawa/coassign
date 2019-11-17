package io.github.misawa.coassign

import io.github.misawa.coassign.collections.IntArrayList

typealias Flow = Int
typealias FlowArray = IntArray
typealias FlowArrayList = IntArrayList
typealias Weight = Int
typealias WeightArray = IntArray
typealias WeightArrayList = IntArrayList
typealias LargeWeight = Long
typealias LargeWeightArray = LongArray

// internal fun Int.toLargeWeight() = this.toLong()
internal fun Weight.toLargeWeight() = this.toLong()
internal fun LargeWeight.toWeight() = this.toInt()
internal fun LargeWeight.toReal() = this.toDouble()
internal fun Double.roundToLargeWeight() = this.toLong()
