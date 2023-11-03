package org.ktlib.trace

import org.ktlib.now
import org.ktlib.nowMillis
import java.time.LocalDateTime
import java.util.*

/**
 * Collects data for a trace statement.
 */
class TraceData(
    var dbTime: Long = 0,
    var dbRequests: Int = 0,
    var duration: Long = 0,
    val extra: MutableMap<String, Any?>,
    var name: String,
    val start: Long = nowMillis(),
    val startTime: LocalDateTime = now(),
    val traceType: String,
) {
    fun end(extra: Map<String, Any?>? = null) {
        if (extra != null) {
            this.extra.putAll(extra)
        }
        duration = nowMillis() - start
    }

    fun addDbTime(time: Long) {
        dbRequests++
        dbTime += time
    }

    override fun toString(): String {
        return "TraceData(traceType='$traceType, name='$name', startTime=$startTime, duration=$duration, dbTime=$dbTime, dbRequests=$dbRequests, extra=$extra')"
    }
}

/**
 * Object that allows you to start and stop a trace in the code.
 */
object Trace {
    private val threadLocal = ThreadLocal<Stack<TraceData>?>()

    private val stack: Stack<TraceData>
        get() = threadLocal.get() ?: Stack<TraceData>().apply { threadLocal.set(this) }

    private var current: TraceData?
        get() = stack.lastOrNull()
        private set(data) {
            if (data != null) {
                stack.push(data)
            }
        }

    init {
        clear()
    }

    fun clear() {
        threadLocal.set(Stack())
    }

    fun start(type: String, name: String, extra: Map<String, Any?> = mapOf()) {
        this.current = TraceData(traceType = type, name = name, extra = extra.toMutableMap())
    }

    fun extra(key: String, value: Any?) {
        this.current?.extra?.put(key, value)
    }

    fun addDbTime(time: Long) {
        stack.forEach { it.addDbTime(time) }
    }

    fun end(extra: Map<String, Any?>? = null) {
        if (stack.size > 1) doEnd(extra)
    }

    private fun doEnd(extra: Map<String, Any?>? = null) {
        stack.pop().apply {
            end(extra)
            TraceLogger.log(this)
        }
    }

    fun finish(name: String, extra: Map<String, Any?>? = null) {
        while (stack.size > 1) end()
        current?.name = name
        doEnd(extra)
    }
}