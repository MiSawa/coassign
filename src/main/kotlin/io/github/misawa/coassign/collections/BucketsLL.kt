package io.github.misawa.coassign.collections

class BucketsLL(val maxBucketId: Int, size: Int) {
    private val bucketEnd = size
    private val bucketIds: IntArray = IntArray(size) { -1 }
    private val first: IntArray = IntArray(maxBucketId + 1) { -1 }
    private val prev: IntArray = IntArray(size + 1) { -1 }
    private val next: IntArray = IntArray(size + 1) { -1 }

    fun clear() {
        bucketIds.fill(-1)
        first.fill(bucketEnd)
        prev.fill(bucketEnd)
        next.fill(bucketEnd)
    }

    fun clear(bucketId: Int) {
        bucketIds.fill(bucketId)
        first.fill(bucketEnd)
        for (i in 0..bucketEnd) {
            prev[i] = i - 1
            next[i] = i + 1
        }
        prev[0] = bucketEnd
        next[bucketEnd] = bucketEnd
    }

    fun getBucket(element: Int) = bucketIds[element]

    fun hasNextElement(bucketId: Int): Boolean = first[bucketId] != bucketEnd
    fun nextElement(bucketId: Int): Int {
        val ret = first[bucketId]
        first[bucketId] = next[ret]
        return ret
    }

    fun setBucket(element: Int, bucketId: Int) {
        val currentBucket = bucketIds[element]
        // Unlink from the current bucket
        if (currentBucket >= 0) {
            if (first[currentBucket] == element) {
                first[currentBucket] = next[element]
            } else {
                val nx = next[element]
                val pr = prev[element]
                prev[nx] = pr
                next[pr] = nx
            }
        }
        bucketIds[element] = bucketId
        val a = first[bucketId]
        prev[a] = element
        next[element] = a
        first[bucketId] = element
    }
}