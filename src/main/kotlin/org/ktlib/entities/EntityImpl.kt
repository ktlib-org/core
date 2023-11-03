package org.ktlib.entities

import org.ktlib.TypeFactory
import org.ktlib.newUUID7
import org.ktlib.now
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.javaSetter

object EntityImplCreator : EntityCreator {
    override fun <T : Any> create(type: KClass<T>) = EntityImpl.create(type)
}

interface EntityMarker {
    fun setProperty(property: KProperty<Any?>, value: Any?)
    fun copy(): Entity
}

internal class EntityImpl(private val type: KClass<*>, private val data: MutableMap<String, Any?> = mutableMapOf()) :
    InvocationHandler {
    companion object {
        private val methodResolution = Collections.synchronizedMap(WeakHashMap<Method, Method>())

        fun <T : Any> create(type: KClass<T>, data: Map<String, Any?> = mapOf()) = createProxy(type, data)

        @Suppress("UNCHECKED_CAST")
        private fun <T : Any> createProxy(type: KClass<T>, data: Map<String, Any?>): T {
            val types = arrayOf(type.java, EntityMarker::class.java)
            val mutableData = data.toMutableMap()
            if (!mutableData.containsKey("id")) {
                mutableData["id"] = newUUID7()
            }
            return Proxy.newProxyInstance(type.java.classLoader, types, EntityImpl(type, mutableData)) as T
        }
    }

    private val lazyEntityValues = mutableMapOf<String, Any?>()

    override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
        val ktProperty = method?.kotlinProperty
        if (ktProperty != null) {
            val (prop, isGetter) = ktProperty
            if (prop.isAbstract) {
                return if (isGetter) {
                    data[prop.name]
                } else {
                    data[prop.name] = args!![0]
                    return null
                }
            }
        }

        return when (method?.declaringClass?.kotlin) {
            EntityMarker::class -> when (method.name) {
                "setProperty" -> {
                    setProperty(args!![0] as KProperty<*>, args[1] as Any?)
                    null
                }

                "copy" -> {
                    create(type, data)
                }

                else -> throw IllegalArgumentException("Unknown method ${method.name}")
            }

            Any::class -> when (method.name) {
                "toString" -> toString()
                "hashCode" -> hashCode()
                "equals" -> this == args!![0]
                else -> throw IllegalArgumentException("Unknown method ${method.name}")
            }

            Entity::class -> when (method.name) {
                "lazyEntityValues" -> lazyEntityValues
                else -> throw IllegalArgumentException("Unknown method ${method.name}")
            }

            else -> try {
                method?.resolve()?.invoke(null, proxy, *(args ?: emptyArray()))
            } catch (e: InvocationTargetException) {
                throw e.targetException
            }
        }
    }

    private val Method.kotlinProperty: Pair<KProperty1<*, *>, Boolean>?
        get() {
            for (prop in declaringClass.kotlin.declaredMemberProperties) {
                if (prop.javaGetter == this) {
                    return Pair(prop, true)
                }
                if (prop is KMutableProperty<*> && prop.javaSetter == this) {
                    return Pair(prop, false)
                }
            }
            return null
        }

    private fun Method.resolve() = methodResolution.computeIfAbsent(this) {
        val implName = "${it.declaringClass.name}\$DefaultImpls"
        val implClass = Class.forName(implName, true, it.declaringClass.classLoader)
        implClass.getMethod(it.name, it.declaringClass, *it.parameterTypes)
    }

    private fun setProperty(property: KProperty<*>, value: Any?) {
        if (!property.returnType.isMarkedNullable && value == null) {
            throw IllegalArgumentException("Property ${property.name} of $type is not nullable, but was set to null")
        }

        if (value != null && value::class != property.returnType.classifier) {
            throw IllegalArgumentException("Property ${property.name} of $type is of type ${property.returnType.classifier}, but was set to ${value::class}")
        }

        data[property.name] = value
    }

    override fun hashCode(): Int {
        var hash = type.hashCode()
        data.forEach { (k, v) ->
            hash = hash * 31 + k.hashCode()
            hash = hash * 31 + v.hashCode()
        }
        return hash
    }

    override fun toString(): String {
        return type.simpleName + data.toString()
    }

    override fun equals(other: Any?): Boolean {
        val target = when (other) {
            is EntityImpl -> other
            is EntityMarker -> Proxy.getInvocationHandler(other) as EntityImpl
            else -> return false
        }

        return type == target.type && data == target.data
    }
}

class EntityStoreTypeFactory(private val type: KClass<EntityStore<*>>) : TypeFactory<EntityStore<*>> {
    private val types = arrayOf(type.java)
    private val loader = type.java.classLoader

    override fun create() = Proxy.newProxyInstance(loader, types, EntityStoreImpl(type)) as EntityStore<*>
}

internal class EntityStoreImpl(private val type: KClass<EntityStore<*>>) : InvocationHandler {
    private val entities = mutableListOf<Entity>()

    override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
        fun Entity.copy() = (this as EntityMarker).copy()
        val entityArg = { (args!![0] as Entity).copy() }

        return when (method?.declaringClass?.kotlin) {
            EntityStore::class -> when (method.name) {
                "create" -> {
                    val entity = entityArg()
                    entity.createdAt = now()
                    entity.updatedAt = entity.createdAt
                    entities.add(entity)
                    entity
                }

                "update" -> {
                    val entity = entityArg()
                    entity.updatedAt = now()
                    entity
                }

                "copy" -> {
                    entityArg()
                }

                "delete" -> {
                    val entity = (args!![0] as Entity)
                    entities.remove(entities.find { it.id == entity.id })
                    entity
                }

                "all" -> {
                    entities.map { it.copy() }
                }

                "deleteAll" -> {
                    @Suppress("UNCHECKED_CAST")
                    val toDelete = args!![0] as List<Entity>
                    toDelete.forEach { entities.remove(it) }
                    toDelete
                }

                "findById" -> {
                    val id = args!![0] as UUID
                    entities.find { it.id == id }?.copy()
                }

                "findByIds" -> {
                    @Suppress("UNCHECKED_CAST")
                    val ids = args!![0] as Collection<UUID>
                    entities.filter { ids.contains(it.id) }.map { it.copy() }
                }

                else -> throw IllegalArgumentException("Unknown method ${method.name}")
            }

            else -> throw IllegalStateException(
                "No mock supplied for ${type.qualifiedName}.${method?.name}(${
                    args?.toList()?.map { it::class.simpleName }?.joinToString(",")
                })"
            )
        }
    }
}
