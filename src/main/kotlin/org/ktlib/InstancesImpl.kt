package org.ktlib

import io.github.oshai.kotlinlogging.KotlinLogging
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

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> instance(type: KClass<T>): T {
        if (!isRegistered(type)) {
            throw NoInstanceException(type)
        }
        val factory = typeFactories[type] ?: findResolver(type)!!(type)
        val instance = factory() as T
        logger.debug { "Returning instance ${instance::class.qualifiedName} for type ${type.qualifiedName}" }
        return instance
    }
}
