package io.github.misawa.coassign.collections

class BucketsArray(maxBucketId: Int, size: Int) {
    private val bucketIds: IntArray = IntArray(size) { -1 }
    private val buckets: Array<IntArrayList> = Array(maxBucketId + 1) { IntArrayList() }

    fun clear() {
        bucketIds.fill(-1)
        buckets.forEach { it.clear() }
    }

    fun clear(bucketId: Int) {
        bucketIds.fill(bucketId)
        buckets.forEach { it.clear() }
        val bucket = buckets[bucketId]
        for (i in bucketIds.indices) {
            bucket.push(i)
        }
    }

    fun getBucket(element: Int) = bucketIds[element]

    fun hasNextElement(bucketId: Int): Boolean {
        val bucket = buckets[bucketId]
        while (bucket.isNotEmpty()) {
            val elem = bucket.top()
            if (bucketIds[elem] == bucketId) return true
            bucket.pop()
        }
        return false
    }

    fun nextElement(bucketId: Int): Int = buckets[bucketId].pop()

    /**
     * The caller is responsible for calling this somewhat monotonically.
     */
    fun setBucket(element: Int, bucketId: Int) {
        val currentBucket = bucketIds[element]
        if (currentBucket == bucketId) return
        bucketIds[element] = bucketId
        buckets[bucketId].push(element)
    }
}