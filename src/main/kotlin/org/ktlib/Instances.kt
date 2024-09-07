package org.ktlib

import kotlin.reflect.KClass

/**
 * A simple type factory that can create an instance of a type
 */
typealias TypeFactory = () -> Any

/**
 * A factory resolver that can supply a factory for all subclasses of a type
 */
typealias FactoryResolver = (type: KClass<*>) -> TypeFactory

/**
 * An exception thrown when no instance is registered for a requested type
 */
class NoInstanceException(val type: KClass<*>, message: String? = null) :
    RuntimeException(message ?: "No instance registered for type ${type.qualifiedName}")

/**
 * This interface defines a basic service locator. It is not intended to be a full-featured DI container.
 * It is used to provide a simple way to lookup implementations of interfaces when they are needed.
 */
interface Instances {
    companion object : Instances by config(key = "instancesImplementation", default = InstancesImpl)

    /**
     * Returns a new instance for the given type.
     * @throws NoInstanceException if no instance is registered for the given type
     * @param type the type to lookup
     * @return a new instance of the given type
     */
    @Throws(NoInstanceException::class)
    fun <T : Any> instance(type: KClass<T>): T

    /**
     * Returns a new instance for the given type.
     * @throws NoInstanceException if no instance is registered for the given type
     * @param type the type to lookup
     * @param default the default instance to return if one has not been registered
     * @return a new instance of the given type
     */
    @Throws(NoInstanceException::class)
    fun <T : Any, D : T> instance(type: KClass<T>, default: D): T

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
    fun registerFactory(type: KClass<*>, factory: TypeFactory)

    /**
     * Register a resolver for any subclass of the specified type
     * @param type the parent type to use this resolver for
     * @param resolver a factory resolver to use to create instances of the type
     */
    fun registerResolver(type: KClass<*>, resolver: FactoryResolver)
}

/**
 * Returns an instance for the given type.
 */
inline fun <reified T : Any> lookupInstance() = Instances.instance(T::class)

/**
 * Returns a new instance for the given type or the default value if no instance is registered for the given type.
 */
inline fun <reified T : Any> lookupInstance(default: T): T {
    if (!T::class.java.isInterface) {
        throw Exception(
            "Lookup type is not an interface, this might be because you didn't invoke this function with " +
                    "the interface type in the function call like lookupInstance<MyInterface>(MyDefaultInstance)"
        )
    }
    return Instances.instance(T::class, default)
}

/**
 * Allows you to register a factory for a type
 */
inline fun <reified T : Any> registerInstanceFactory(noinline factory: () -> T) {
    if (!T::class.java.isInterface) {
        throw Exception(
            "Registration type is not an interface, this might be because you didn't invoke this function with " +
                    "the interface type in the function call like registerInstanceFactory<MyInterface> { }"
        )
    }
    Instances.registerFactory(T::class, factory)
}
