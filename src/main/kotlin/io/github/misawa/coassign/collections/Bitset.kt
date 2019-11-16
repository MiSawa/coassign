package io.github.misawa.coassign.collections

private typealias Word = Long
private typealias WordArray = LongArray

private const val NON_BIT_WORD: Word = 0
private const val ONE_BIT_WORD: Word = 1
private const val ALL_BIT_WORD: Word = Word.MAX_VALUE
private const val BITS_PER_WORD: Int = Word.SIZE_BITS
private const val WORD_MASK: Int = Word.SIZE_BITS - 1
private const val WORD_SHIFT: Int = 6 // log_2 BITS_PER_WORD
private fun Int.toWord(): Word = this.toLong()

@ExperimentalStdlibApi
class Bitset(val length: Int) {
    private val bits: WordArray

    init {
        val numWords = (length + BITS_PER_WORD - 1) / BITS_PER_WORD
        bits = WordArray(numWords)
    }

    private fun Int.head(): Int = this shr WORD_SHIFT
    private fun Int.tail(): Int = this and WORD_MASK
    private fun Int.tailBit(): Word = (1.toWord()) shl this.tail()

    private fun checkRange(element: Int): Int = element.also {
        check(element in 0 until length) {
            "Element $element out of range [0, $length)"
        }
    }

    fun add(element: Int) {
        val head = element.head()
        bits[head] = bits[head] or element.tailBit()
    }

    fun clear() {
        bits.fill(NON_BIT_WORD)
    }

    fun remove(element: Int) {
        val head = element.head()
        bits[head] = bits[head] and element.tailBit().inv()
    }

    fun contains(element: Int): Boolean {
        return ((bits[element.head()] shr element.tail()) and 1) == 1.toWord()
    }

    fun setRange(start: Int, endExclusive: Int) {
        val endInclusive = endExclusive.dec()
        val startHead = start.head()
        var endHead = endInclusive.head()
        if (startHead == endHead) {

        } else {
            bits[startHead] = bits[startHead] or (ALL_BIT_WORD shl start.tail())
            if (endExclusive.tail() == 0) {
                ++endHead
            } else {
                bits[endHead] = bits[endHead] or ((ONE_BIT_WORD shl endInclusive.tail()) - 1)
            }
            for (i in (startHead + 1) until endHead) {
                bits[i] = ALL_BIT_WORD
            }
        }
    }

    /**
     * Returns -1 if there is no such a thing
     */
    fun firstSetBit(start: Int): Int {
        val head = start.head()
        val firstWordRemaining = bits[head] ushr start.tail()
        if (firstWordRemaining != NON_BIT_WORD) {
            return start + firstWordRemaining.countTrailingZeroBits()
        } else {
            for (i in (head + 1) until bits.size) {
                if (bits[i] != NON_BIT_WORD) {
                    return (i shl WORD_SHIFT) + bits[i].countTrailingZeroBits()
                }
            }
            return -1
        }
    }

    /**
     * Returns -1 if there is no such a thing
     */
    fun firstSetBitInRange(start: Int, endExclusive: Int): Int {
        val head = start.head()
        val firstWordRemaining = bits[head] shr start.tail()
        if (firstWordRemaining != NON_BIT_WORD) {
            return start + firstWordRemaining.countTrailingZeroBits()
        } else {
            val lastHead = (endExclusive - 1).head()
            for (i in (head + 1) until lastHead) {
                if (bits[i] != NON_BIT_WORD) {
                    return (i shl WORD_SHIFT) + bits[i].countTrailingZeroBits()
                }
            }
            if (bits[lastHead] != NON_BIT_WORD) {
                val ret = (lastHead shl WORD_SHIFT) + bits[lastHead].countTrailingZeroBits()
                if (ret < endExclusive) return ret
            }
            return -1
        }
    }
}