package org.ktlib

import io.github.oshai.kotlinlogging.KotlinLogging
import org.ktlib.entities.Entity
import org.ktlib.entities.Repository
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

internal object InstancesImpl : Instances {
    private val logger = KotlinLogging.logger {}
    private val typeFactories = mutableMapOf<KClass<*>, TypeFactory>()
    private val factoryResolvers = mutableMapOf<KClass<*>, FactoryResolver>()
    private val factoryResolverByType = mutableMapOf<KClass<*>, FactoryResolver>()

    override fun isRegistered(type: KClass<*>): Boolean {
        return typeFactories.containsKey(type) || findResolver(type) != null
    }

    private fun findResolver(type: KClass<*>): FactoryResolver? {
        return factoryResolverByType[type] ?: factoryResolvers.keys.find { type.isSubclassOf(it) }
            ?.let { factoryResolvers[it] }
            ?.also { factoryResolverByType[type] = it }
    }

    override fun registerFactory(type: KClass<*>, factory: TypeFactory) {
        doRegister(type, factory)
    }

    private fun doRegister(type: KClass<*>, factory: TypeFactory) {
        logger.debug { "Registering factory ${typeName(factory::class.qualifiedName)}for interface ${type.qualifiedName}" }
        typeFactories[type] = factory
    }

    private fun typeName(name: String?) = when {
        name == null -> ""
        name.contains("\$\$Lambda\$") -> "${name.substringBefore("$$")}.Lambda "
        else -> "$name "
    }

    override fun registerResolver(type: KClass<*>, resolver: FactoryResolver) {
        logger.debug { "Registering resolver ${typeName(resolver::class.qualifiedName)}for interface ${type.qualifiedName}" }
        factoryResolvers[type] = resolver
    }

    override fun <T : Any> instance(type: KClass<T>): T {
        logger.debug { "Returning implementation for $type" }

        return if (isRegistered(type)) {
            getInstance(type)
        } else {
            // This will allow injection of interfaces that are not registered yet.
            // It will resolve the instance once it's actually needed, and fail then if one isn't registered.
            @Suppress("UNCHECKED_CAST")
            Proxy.newProxyInstance(type.java.classLoader, arrayOf(type.java), InvocationHandlerImp(type)) as T
        }
    }

    class InvocationHandlerImp<T : Any>(private val type: KClass<T>) : InvocationHandler {
        private val instance: T by lazy { getInstance(type) }

        override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
            try {
                return method?.invoke(instance, *(args ?: arrayOf()))
            } catch (e: InvocationTargetException) {
                throw e.targetException
            } catch (e: NoInstanceException) {
                var message = "No factory found when trying to invoke ${type.qualifiedName}.${method?.name}()"
                if (e.stackTraceToString().contains("io.mockk.")) {
                    message += ".\nThis might be because you forgot to mock the '${method?.name}' method!"
                } else if (e.type.isSubclassOf(Repository::class) || e.type.isSubclassOf(Entity::class)) {
                    message += ".\nHave you initialized the Entity classes by calling EntityInitializer.init()?"
                }
                throw NoInstanceException(e.type, message)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> getInstance(type: KClass<T>): T {
        if (!isRegistered(type)) {
            throw NoInstanceException(type)
        }
        val factory = typeFactories[type] ?: findResolver(type)!!(type)
        return factory() as T
    }
}
