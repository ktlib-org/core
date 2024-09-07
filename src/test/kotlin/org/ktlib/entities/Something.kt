package org.ktlib.entities

import org.ktlib.entities.Validation.field
import org.ktlib.entities.Validation.notBlank
import org.ktlib.entities.Validation.validate
import org.ktlib.lookupInstance
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

enum class MyEnum {
    One, Two
}

interface Something : Entity {
    companion object : Factory<Something>()

    var name: String
    var value: String?
    var enabled: Boolean
    var num: Int
    var date: LocalDate
    var dateTime: LocalDateTime
    var enum: MyEnum
    var long: Long?
    val immutalbe: Long?

    fun validate() = validate {
        field(Something::name) { notBlank() }
    }

    val aLazyValue: String get() = lazyValue(::aLazyValue) { UUID.randomUUID().toString() }
}

data class SomethingInfo(
    val num: Int,
    val name: String,
    val date: LocalDate,
    val dateTime: LocalDateTime,
    val enum: MyEnum,
    var long: Long?
)

object Somethings : SomethingRepository by lookupInstance()

interface SomethingRepository : Repository<Something> {
    fun findByLotsOfThings(name: String, count: Int, date: LocalDateTime): List<Something>

    fun create(name: String) = Something {
        this.name = name
        set(this::long to 5)
    }.create()
}
