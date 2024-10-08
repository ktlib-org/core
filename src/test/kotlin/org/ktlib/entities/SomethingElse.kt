package org.ktlib.entities

import org.ktlib.entities.Validation.field
import org.ktlib.entities.Validation.notBlank
import org.ktlib.entities.Validation.notNull
import org.ktlib.entities.Validation.validate
import org.ktlib.lookupInstance
import java.util.*

interface SomethingElse : Entity {
    companion object : Factory<SomethingElse>()

    var somethingId: UUID
    var name: String


    fun validate() = validate {
        field(this::name) { notBlank() }
        field(this::somethingId) { notNull() }
    }

    val something: Something
        get() = lazyValue(::something) { Somethings.findById(this.somethingId)!! }

    val somethingNull: Something?
        get() = lazyValue(::somethingNull) { Somethings.findById(this.somethingId) }

    val somethings: List<Something>
        get() = lazyValue(::somethings) { Somethings.findByIds(listOf(this.somethingId)) }
}

fun List<SomethingElse>.preloadSomething() = preloadLazyValue(
    SomethingElse::something,
    { Somethings.findByIds(map { it.somethingId }) },
    { one, many -> many.find { it.id == one.somethingId }!! })

fun List<SomethingElse>.preloadSomethingNull() = preloadLazyValue(
    SomethingElse::somethingNull,
    { Somethings.findByIds(map { it.somethingId }) },
    { _, _ -> null })

fun List<SomethingElse>.preloadSomethings() = preloadLazyList(
    SomethingElse::somethings,
    { Somethings.findByIds(map { it.somethingId }) },
    { one, many -> many.filter { it.id == one.somethingId } })

object SomethingElses : SomethingElseRepository by lookupInstance()

interface SomethingElseRepository : Repository<SomethingElse>
