package org.ktlib.trace

import org.ktlib.newUUID4
import org.ktlib.nowMillis
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

/**
 * Collects data for a trace statement.
 */
class TraceData(
    val id: UUID = newUUID4(),
    val sessionId: UUID?,
    val parentId: UUID?,
    val correlationId: UUID,
    var dbTime: Long = 0,
    var dbRequests: Int = 0,
    val extra: MutableMap<String, Any?>,
    var name: String,
    val start: Long = nowMillis(),
    var end: Long = start,
    val traceType: String,
) {
    val duration: Long
        get() = start - end

    val startTime: LocalDateTime
        get() = LocalDateTime.ofInstant(Instant.ofEpochMilli(start), ZoneId.systemDefault())

    fun end(extra: Map<String, Any?>? = null) {
        if (extra != null) {
            this.extra.putAll(extra)
        }
        end = nowMillis()
    }

    fun addDbTime(time: Long) {
        dbRequests++
        dbTime += time
    }

    override fun toString(): String {
        return "TraceData(sessionId='$sessionId', correlationId='$correlationId', parentId='$parentId', id='$id', traceType='$traceType, name='$name', start=$start, end=$end, dbTime=$dbTime, dbRequests=$dbRequests, extra=$extra')"
    }
}

/**
 * Object that allows you to start and stop a trace in the code.
 */
object Trace {
    private val threadLocal = ThreadLocal<Stack<TraceData>?>()
    private val sessionId = ThreadLocal<UUID>()

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

    fun sessionId(id: UUID) {
        sessionId.set(id)
    }

    fun clear() {
        sessionId.set(null)
        threadLocal.set(Stack())
    }

    fun start(type: String, name: String, extra: Map<String, Any?> = mapOf()) {
        this.current = TraceData(
            sessionId = this.current?.sessionId ?: this.sessionId.get(),
            correlationId = this.current?.correlationId ?: newUUID4(),
            parentId = this.current?.id,
            traceType = type,
            name = name,
            extra = extra.toMutableMap()
        )
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