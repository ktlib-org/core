package org.ktlib

import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.isSubclassOf

/**
 * A simple type factory that can create an instance of a type
 */
interface TypeFactory<T : Any> {
    companion object {
        fun <T : Any> defaultFactory(type: KClass<*>): TypeFactory<T> {
            return object : TypeFactory<T> {
                private var instanceValidated = false

                override fun create(): T {
                    return checkInstance(type.objectInstance ?: type.createInstance())
                }

                private fun checkInstance(instance: Any): T {
                    if (!instanceValidated) {
                        synchronized(this) {
                            if (!instance::class.isSubclassOf(typeClass)) {
                                throw RuntimeException("The factory ${this::class.qualifiedName} returned a instance of type ${instance::class.qualifiedName} which is not a subclass of ${typeClass.qualifiedName}")
                            }
                            instanceValidated = true
                        }
                    }
                    @Suppress("UNCHECKED_CAST")
                    return instance as T
                }

                private val typeClass by lazy {
                    @Suppress("UNCHECKED_CAST")
                    typeArguments(TypeFactory::class)[0] as KClass<T>
                }
            }
        }
    }

    fun create(): T
}

/**
 * A factory resolver that can supply a factory for all subclasses of a type
 */
interface FactoryResolver {
    fun <F : Any> resolve(type: KClass<F>): TypeFactory<F>
}

/**
 * An exception thrown when no instance is registered for a requested type
 */
class NoInstanceException(val type: KClass<*>, message: String? = null) :
    RuntimeException(message ?: "No instance registered for type ${type.qualifiedName}")

/**
 * This interface defines a basic service locator. It is not intended to be a full-featured DI container.
 * It is used to provide a simple way to lookup implementations of interfaces when they are needed
 */
interface Instances {
    companion object : Instances by config("instancesImplementation", InstancesImpl)

    /**
     * Returns a new instance for the given type.
     * @throws NoInstanceException if no instance is registered for the given type
     * @param type the type to lookup
     * @return a new instance of the given type
     */
    @Throws(NoInstanceException::class)
    fun <T : Any> instance(type: KClass<T>): T

    /**
     * Returns true if a factory has been registered for the given type
     * @param type the type to lookup
     * @return true if a factory has been registered for the given type
     */
    fun isRegistered(type: KClass<*>): Boolean

    /**
     * Register a factory for a type
     * @param type the type whose factory
     * @param factory a factory to use to create instances of the type
     */
    fun <T : Any> register(type: KClass<T>, factory: TypeFactory<T>)

    /**
     * Register a resolver for any subclass of the specified type
     * @param type the parent type to use this resolver for
     * @param resolver a factory resolver to use to create instances of the type
     */
    fun <T : Any> register(type: KClass<T>, resolver: FactoryResolver)
}

/**
 * Returns a new instance for the given type.
 */
inline fun <reified T : Any> lookup() = Instances.instance(T::class)

/**
 * Returns a new instance for the given type or the default value if no instance is registered for the given type.
 */
inline fun <reified T : Any> lookup(default: T) =
    if (Instances.isRegistered(T::class)) Instances.instance(T::class) else default