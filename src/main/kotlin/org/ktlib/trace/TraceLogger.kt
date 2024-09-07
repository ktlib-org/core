package org.ktlib.trace

import org.ktlib.lookupInstance
import java.time.LocalDateTime

/**
 * Interface for logging trace data
 */
interface TraceLogger {
    companion object : TraceLogger by lookupInstance(default = ConsoleLogger)

    /**
     * @param trace The trace data that should be logged
     */
    fun log(trace: TraceData)

    /**
     * @param dateRange only return traces within this date range
     * @param type only return traces with this type
     * @param name only return traces with this name
     * @param durationMin only return traces with duration of at least this much
     * @param dbTimeMin only return traces with DB time of at lease this much
     */
    fun loadTraceData(
        dateRange: ClosedRange<LocalDateTime>?,
        type: String?,
        name: String?,
        durationMin: Int?,
        dbTimeMin: Int?
    ): List<TraceData>
}