package org.ktlib.trace

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.semconv.ServiceAttributes
import org.ktlib.Environment
import org.ktlib.config


/**
 * Object that wraps OpenTelemetry to make creating, starting, and stopping spans a little easier.
 */
object Trace {
    private val appName = config("applicationName", "MyApplication")
    private val openTelemetry: OpenTelemetry by lazy {
        val resource = Resource.getDefault().toBuilder()
            .put(ServiceAttributes.SERVICE_NAME, appName)
            .put(ServiceAttributes.SERVICE_VERSION, Environment.version ?: "Unknown")
            .build()
        config<TraceSupplier>("traceSupplier", ConsoleTrace).supply(resource)
    }

    private val tracer: Tracer
        get() = openTelemetry.getTracer(appName)

    fun start(name: String, attributes: Map<String, Any?> = mapOf()) {
        tracer.spanBuilder(name).startSpan().setAttributes(attributes).makeCurrent()
    }

    fun updateName(name: String) {
        Span.current().updateName(name)
    }

    private fun Span.setAttribute(name: String, value: Any?) = apply {
        this.setAttribute(name, value?.toString() ?: "")
    }

    private fun Span.setAttributes(extra: Map<String, Any?>?) = apply {
        extra?.entries?.forEach { this.setAttribute(it.key, it.value) }
    }

    fun attribute(key: String, value: Any?) {
        Span.current().setAttribute(key, value)
    }

    fun buildAttributes(service: String, resource: String): MutableMap<String, Any?> {
        return mutableMapOf(ServiceAttributes.SERVICE_NAME.key to service, "resource.name" to resource)
    }

    fun error(throwable: Throwable, status: String? = null) {
        Span.current().setStatus(StatusCode.ERROR, status ?: "").recordException(throwable)
    }

    fun end(extra: Map<String, Any?>? = null) {
        Span.current().setAttributes(extra)
    }
}

fun <T> withTrace(name: String, extra: Map<String, Any?> = mapOf(), block: () -> T): T {
    Trace.start(name, extra)
    return try {
        block()
    } catch (t: Throwable) {
        Trace.error(t)
        throw t
    } finally {
        Trace.end()
    }
}
