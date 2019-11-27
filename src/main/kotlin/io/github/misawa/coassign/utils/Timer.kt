package io.github.misawa.coassign.utils

import java.io.Closeable

interface Timer {
    fun <T> timed(inner: () -> T): T
    fun start(): Closeable
    fun stop()
}

class TimerImpl : Timer {
    private var totalTime: Long = 0
    private var count: Int = 0
    private var start: Long = -1

    companion object {
        private fun now(): Long = System.currentTimeMillis()
    }

    override fun <T> timed(inner: () -> T): T {
        val start = now()
        val end = now()
        val ret = inner.invoke()
        ++count
        totalTime += end - start
        return ret
    }

    override fun start(): Closeable {
        check(start == -1L)
        start = now()
        return Closeable { stop() }
    }

    override fun stop() {
        val end = now()
        check(start >= 0)
        ++count
        totalTime += end - start
        start = -1
    }

    override fun toString(): String{
        check(start == -1L)
        return "Timer{ totalTIme=$totalTime, count=$count }"
    }
}

object NOOPTimer : Timer {
    override fun <T> timed(inner: () -> T): T = inner.invoke()
    override fun start(): Closeable = Closeable {}
    override fun stop() {}
}

class StatsTracker(private val supplier: () -> Timer = { NOOPTimer }) {
    companion object {
        fun noop(): StatsTracker = StatsTracker()
        fun millis(): StatsTracker = StatsTracker { TimerImpl() }
    }

    private val timers: LinkedHashMap<String, Timer> = LinkedHashMap()
    operator fun get(name: String) = timers.computeIfAbsent(name) { supplier() }

    fun getStats() = timers
}