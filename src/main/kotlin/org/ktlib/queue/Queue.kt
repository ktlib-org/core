package org.ktlib.queue

import org.ktlib.Json
import org.ktlib.TypeRef
import org.ktlib.lookup
import org.ktlib.typeRef

/**
 * Interface for interacting with a Queuing system.
 */
interface Queue {
    companion object : Queue by lookup()

    enum class HandlerResult { Ack, NackRequeue, NackNoRequeue }

    val connected: Boolean

    fun createQueue(queueName: String, durable: Boolean = true, exclusive: Boolean = false, autoDelete: Boolean = false)

    fun ensureQueueExists(
        queueName: String,
        durable: Boolean = true,
        exclusive: Boolean = false,
        autoDelete: Boolean = false
    ) {
        if (!exists(queueName)) {
            createQueue(queueName, durable, exclusive, autoDelete)
        }
    }

    fun exists(queueName: String): Boolean

    fun <T : Any> listen(queueName: String, handler: (T) -> HandlerResult, messageClass: TypeRef<T>): String

    fun <T : Any> listen(queueName: String, handler: (T) -> HandlerResult, messageClass: TypeRef<T>, count: Int) =
        (1..count).map {
            listen(queueName, handler, messageClass)
        }

    fun messageCount(queueName: String): Long

    fun publish(queueName: String, data: String) {
        publish(queueName, data.toByteArray())
    }

    fun publish(queueName: String, data: ByteArray)

    fun publish(queueName: String, data: Any) {
        publish(queueName, Json.serializeAsBytes(data))
    }

    fun removeQueue(queueName: String, onlyIfEmpty: Boolean = false, onlyIfUnused: Boolean = false)
    fun stopListening(consumerTag: String)
}

inline fun <reified T> Queue.listen(queueName: String, noinline handler: (T) -> Queue.HandlerResult) =
    listen(queueName, handler, typeRef())

inline fun <reified T> Queue.listen(queueName: String, noinline handler: (T) -> Queue.HandlerResult, count: Int) =
    listen(queueName, handler, typeRef(), count)
