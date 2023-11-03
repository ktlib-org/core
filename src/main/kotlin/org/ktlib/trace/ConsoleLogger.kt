package org.ktlib.trace

import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDateTime

object ConsoleLogger : TraceLogger {
    private val logger = KotlinLogging.logger("TraceLogger")

    override fun log(trace: TraceData) {
        logger.warn { trace.toString() }
    }

    override fun loadTraceData(
        dateRange: ClosedRange<LocalDateTime>?,
        type: String?,
        name: String?,
        durationMin: Int?,
        dbTimeMin: Int?
    ) = listOf<TraceData>()
}