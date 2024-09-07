package org.ktlib.entities

import org.ktlib.lookupInstance
import org.ktlib.toUUID
import org.ktlib.typeArguments
import java.io.Closeable
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.staticFunctions
import kotlin.reflect.jvm.jvmErasure

interface Entity {
    val id: UUID
    var createdAt: LocalDateTime
    var updatedAt: LocalDateTime

    fun lazyEntityValues(): MutableMap<String, Any?>
}

interface Repository<T : Entity> {
    /**
     * Create a copy of this entity.
     */
    fun T.copy(): T

    /**
     * Create entity in the repository.
     */
    fun T.create(): T

    /**
     * Update entity in the repository.
     */
    fun T.update(): T

    /**
     * Delete entity from the repository.
     */
    fun T.delete(): T

    /**
     * Returns all instances of an entity in the repository.
     */
    fun all(): List<T>

    /**
     * Delete all entities from the repository.
     */
    fun List<T>.deleteAll(): List<T>

    /**
     * Find entity by its id.
     */
    fun findById(id: UUID?): T?

    /**
     * Find entities by their ids.
     */
    fun findByIds(ids: Collection<UUID>): List<T>


    fun <T : Entity, P> T.set(v: Pair<KProperty0<P>, P>) = forceSetEntityProperty(this, v)
}

internal fun <T : Entity> forceSetEntityProperty(entity: T, v: Pair<KProperty0<Any?>, Any?>): T {
    val (prop, pairValue) = v

    val value = when {
        pairValue is Int && prop.returnType.classifier == Long::class -> pairValue.toLong()
        else -> pairValue
    }

    if (!prop.returnType.isMarkedNullable && value == null) {
        throw IllegalArgumentException("Cannot set null value on non-nullable property ${prop.name}")
    } else if (value != null && prop.returnType.classifier != value::class) {
        throw IllegalArgumentException("Cannot set value of type ${value::class} on property ${prop.name} of type ${prop.returnType.classifier}")
    }

    when (entity) {
        is EntityMarker -> entity.setProperty(prop, value)
        else -> throw IllegalStateException("Cannot use set on class ${entity::class}")
    }

    return entity
}

fun List<Entity>.ids() = this.map { it.id }

fun <T : Entity> T.populateFrom(data: Map<String, Any?>, dataClass: KClass<*>) = apply {
    dataClass.memberProperties.map { dataProperty ->
        if (data.containsKey(dataProperty.name)) {
            val returnType = dataProperty.returnType.jvmErasure
            val value = data[dataProperty.name]
            val property = this::class.memberProperties.find { it.name == dataProperty.name }
            if (property is KMutableProperty<*>) {
                val newValue = when {
                    value is String && returnType.isSubclassOf(Enum::class) ->
                        returnType.staticFunctions.find { f -> f.name == "valueOf" }!!.call(value)

                    value is String && returnType == LocalDate::class -> LocalDate.parse(value)

                    value is String && returnType == LocalDateTime::class -> LocalDateTime.parse(value)

                    value is Int && returnType == Long::class -> value.toLong()

                    value is Float && returnType == Double::class -> value.toDouble()

                    value is String && returnType == UUID::class -> value.toUUID()

                    else -> value
                }

                property.setter.call(this, newValue)
            }
        }
    }
}

fun <T : Entity> T.populateFrom(data: Any) = this.apply {
    data::class.memberProperties.forEach { dataProperty ->
        val property = this::class.memberProperties.find { it.name == dataProperty.name }
        if (property is KMutableProperty<*>) {
            property.setter.call(this, dataProperty.getter.call(data))
        }
    }
}

fun <T> Entity.lazyValue(prop: KProperty<T>, loader: () -> T): T {
    if (!isLazyValueLoaded(prop)) {
        lazyEntityValues()[prop.name] = loader()
    }

    @Suppress("UNCHECKED_CAST")
    return lazyEntityValues()[prop.name] as T
}

fun <T> Entity.ifLazyAssociationLoaded(prop: KProperty<*>, block: () -> T?) =
    if (isLazyValueLoaded(prop)) block() else null

fun Entity.isLazyValueLoaded(prop: KProperty<*>) = lazyEntityValues().containsKey(prop.name)

fun <T : Entity> T.clearLazyValue(prop: KProperty<*>) = apply {
    lazyEntityValues().remove(prop.name)
}

fun <T : Entity> List<T>.clearLazyValue(prop: KProperty<*>) = onEach { it.clearLazyValue(prop) }

fun <T : Entity> T.clearLazyValues() {
    lazyEntityValues().clear()
}

fun <T : Entity> List<T>.clearLazyValues() = onEach { it.lazyEntityValues().clear() }

fun <T : Entity, E> List<T>.preloadLazyValue(
    prop: KProperty<E>,
    lookup: List<T>.() -> List<E>,
    mapper: (T, List<E>) -> E,
): List<T> {
    val filteredList = this.filter { !it.isLazyValueLoaded(prop) }

    if (filteredList.isEmpty()) return this

    val items = filteredList.lookup()

    filteredList.forEach { it.lazyEntityValues()[prop.name] = mapper(it, items) }

    return this
}

fun <T : Entity, E> List<T>.preloadLazyList(
    prop: KProperty<List<E>>,
    lookup: List<T>.() -> List<E>,
    mapper: (T, List<E>) -> List<E>,
): List<T> {
    @Suppress("UNCHECKED_CAST")
    return preloadLazyValue(prop as KProperty<E>, lookup, mapper as (T, List<E>) -> E)
}

abstract class Factory<E : Entity> {
    operator fun invoke(): E = EntityCreator.create(instanceClass)

    operator fun invoke(init: E.() -> Unit): E = invoke().apply(init)

    private val instanceClass by lazy {
        @Suppress("UNCHECKED_CAST")
        typeArguments(Factory::class)[0] as KClass<E>
    }
}

interface EntityCreator {
    companion object : EntityCreator by lookupInstance<EntityCreator>(default = EntityImplCreator)

    fun <T : Any> create(type: KClass<T>): T
}

interface TransactionManager : Closeable {
    companion object : TransactionManager by lookupInstance<TransactionManager>(default = EmptyTransactionManager)

    fun <T> runInTransaction(func: () -> T): T
    fun startTransaction()
    fun rollback()
}

internal object EmptyTransactionManager : TransactionManager {
    override fun <T> runInTransaction(func: () -> T): T = func()
    override fun startTransaction() {}
    override fun rollback() {}
    override fun close() {}
}

fun <T> transaction(func: () -> T) = TransactionManager.runInTransaction(func)