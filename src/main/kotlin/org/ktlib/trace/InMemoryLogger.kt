package org.ktlib.trace

import java.time.LocalDateTime

object InMemoryLogger : TraceLogger {
    private val traces = mutableListOf<TraceData>()

    override fun loadTraceData(
        dateRange: ClosedRange<LocalDateTime>?,
        type: String?,
        name: String?,
        durationMin: Int?,
        dbTimeMin: Int?
    ) = synchronized(traces) {
        traces.filter {
            (type == null || it.traceType == type) ||
                    (name == null || it.name == name) ||
                    (durationMin == null || it.duration >= durationMin) ||
                    (dbTimeMin == null || it.dbTime >= dbTimeMin) ||
                    (dateRange == null || dateRange.contains(it.startTime))

        }
    }

    override fun log(trace: TraceData) = synchronized(traces) {
        traces.add(trace)
        ConsoleLogger.log(trace)
    }
}