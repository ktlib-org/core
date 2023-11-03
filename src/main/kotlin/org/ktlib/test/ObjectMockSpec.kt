package org.ktlib.test

import io.kotest.core.spec.DslDrivenSpec
import io.kotest.core.spec.style.scopes.StringSpecRootScope
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.platform.commons.annotation.Testable

@Testable
abstract class ObjectMockSpec(body: ObjectMockSpec.() -> Unit = {}) : DslDrivenSpec(), StringSpecRootScope {
    private val objectMocks = mutableListOf<Any>()

    fun objectMocks(vararg objects: Any) {
        objectMocks.addAll(objects.toList())
    }

    init {
        beforeEach {
            objectMocks.forEach { mockkObject(it) }
        }

        afterEach {
            if (objectMocks.isNotEmpty()) {
                unmockkAll()
            }
        }

        body()
    }
}
