package org.ktlib.test

import io.kotest.core.spec.DslDrivenSpec
import io.kotest.core.spec.style.scopes.StringSpecRootScope
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.platform.commons.annotation.Testable
import org.ktlib.Instances
import org.ktlib.entities.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty0

@Testable
abstract class EntitySpec(body: EntitySpec.() -> Unit = {}) : DslDrivenSpec(), StringSpecRootScope {
    private val objectMocks = mutableListOf<Any>()

    companion object {
        fun useTestEntityStores() {
            if (!Instances.isRegistered(EntityStore::class)) {
                @Suppress("UNCHECKED_CAST")
                Instances.registerResolver(EntityStore::class) { EntityStoreTypeFactory(it as KClass<EntityStore<*>>) }
            }
        }
    }

    init {
        useTestEntityStores()

        beforeEach {
            objectMocks.forEach { mockkObject(it) }
            TransactionManager.startTransaction()
        }

        afterEach {
            TransactionManager.rollback()
            TransactionManager.close()
            if (objectMocks.isNotEmpty()) {
                unmockkAll()
            }
        }

        body()
    }

    fun objectMocks(vararg objects: Any) {
        objectMocks.addAll(objects.toList())
    }

    fun <T : Entity, P> T.set(v: Pair<KProperty0<P>, P>) = forceSetEntityProperty(this, v)
}
