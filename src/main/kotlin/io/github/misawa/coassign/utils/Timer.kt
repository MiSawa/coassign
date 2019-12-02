package io.github.misawa.coassign.utils

import java.io.Closeable

class Timer(val noop: Boolean) {
    var totalTime: Long = 0
    var count: Int = 0
    var start: Long = -1

    companion object {
        fun now(): Long = System.currentTimeMillis()
    }

    inline fun <T> timed(inner: () -> T): T {
        if (noop) return inner.invoke()
        val start = now()
        try {
            return inner.invoke()
        } finally {
            val end = now()
            ++count
            totalTime += end - start
        }
    }

    fun start(): Closeable {
        if (noop) return Closeable {}
        check(start == -1L)
        start = now()
        return Closeable { stop() }
    }

    fun stop() {
        if (noop) return
        val end = now()
        check(start >= 0)
        ++count
        totalTime += end - start
        start = -1
    }

    override fun toString(): String {
        check(start == -1L)
        return "Timer{ totalTIme=$totalTime, count=$count }"
    }
}

interface Counter {
    fun inc()
    fun inc(incr: Int)
}

private object NOOPCounter : Counter {
    override fun inc() {}
    override fun inc(incr: Int) {}
}

private class CounterImpl : Counter {
    var count = 0

    override fun inc() {
        ++count
    }

    override fun inc(incr: Int) {
        count += incr
    }

    override fun toString(): String = "Counter{ count=$count }"
}

class StatsTracker(private val noop: Boolean) {
    companion object {
        fun noop(): StatsTracker = StatsTracker(true)
        fun millis(): StatsTracker = StatsTracker(false)
    }

    private val timers: LinkedHashMap<String, Timer> = LinkedHashMap()
    private val counters: LinkedHashMap<String, Counter> = LinkedHashMap()
    fun timer(name: String) = timers.computeIfAbsent(name) { Timer(noop) }
    fun counter(name: String) = counters.computeIfAbsent(name) { if (noop) NOOPCounter else CounterImpl() }

    fun getTimeStats() = timers
    fun getCounts() = counters
}