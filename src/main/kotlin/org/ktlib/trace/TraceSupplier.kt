package org.ktlib.trace

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.sdk.resources.Resource

interface TraceSupplier {
    fun supply(resource: Resource): OpenTelemetry
}