package org.ktlib.cache

import org.ktlib.toJson
import java.util.*

object InMemoryCache : Cache {
    private val cache = mutableMapOf<String, String>()
    override val connected = true

    override fun delete(key: String) {
        cache.remove(key)
    }

    override fun set(key: String, value: Any, ttlSecond: Long?) {
        cache[key] = convertValue(value)
        if (ttlSecond != null) {
            Timer().schedule(object : TimerTask() {
                override fun run() {
                    delete(key)
                }
            }, ttlSecond * 1000)
        }
    }

    private fun convertValue(value: Any) = if (value is String) value else value.toJson()

    override fun add(key: String, value: Any, ttlSecond: Long?) {
        if (!cache.containsKey(key)) {
            set(key, value, ttlSecond)
        }
    }

    override fun update(key: String, value: Any, ttlSecond: Long?) {
        if (cache.containsKey(key)) {
            set(key, value, ttlSecond)
        }
    }

    override fun get(key: String) = cache[key]
}