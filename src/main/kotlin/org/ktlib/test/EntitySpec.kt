package org.ktlib.test

import io.kotest.core.spec.DslDrivenSpec
import io.kotest.core.spec.style.scopes.StringSpecRootScope
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.platform.commons.annotation.Testable
import org.ktlib.Instances
import org.ktlib.entities.*
import kotlin.reflect.KProperty0

@Testable
abstract class EntitySpec(body: EntitySpec.() -> Unit = {}) : DslDrivenSpec(), StringSpecRootScope {
    private val objectMocks = mutableListOf<Any>()

    init {
        EntityInitializer.init()
        Instances.register(EntityStore::class, EntityStoreResolver)

        beforeEach {
            objectMocks.forEach { mockkObject(it) }
            TransactionManager.runInTransaction()
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

    fun <T : Entity> T.set(v: Pair<KProperty0<Any?>, Any?>): T {
        val (prop, pairValue) = v

        val value = when {
            pairValue is Int && prop.returnType.classifier == Long::class -> pairValue.toLong()
            else -> pairValue
        }

        when (this) {
            is EntityMarker -> this.setProperty(prop, value)
            else -> throw IllegalStateException("Cannot use set on class ${this::class}")
        }

        return this
    }
}
