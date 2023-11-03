package org.ktlib.entities

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import org.ktlib.entities.SomethingElses.create
import org.ktlib.entities.Somethings.create
import org.ktlib.newUUID4
import org.ktlib.newUUID7
import org.ktlib.now
import org.ktlib.test.EntitySpec
import java.time.LocalDate
import java.time.LocalDateTime

class EntityTests : EntitySpec({
    objectMocks(Somethings)

    val somethingId = newUUID7()
    val somethingElseId = newUUID4()

    beforeEach {
        Something {
            set(this::id to somethingId)
            name = "FirstValue"
            enabled = true
        }.create()
        SomethingElse {
            set(this::id to somethingElseId)
            name = "FirstElse"
            this.somethingId = somethingId
        }.create()
    }

    "can load by id" {
        val s = Somethings.findById(somethingId)

        s?.name shouldBe "FirstValue"
    }

    "can save entity" {
        val s = Something {
            name = "MyName"
        }.create()

        s.name shouldBe "MyName"
    }

    "lazy loaded value" {
        val s = Something {}

        val value = s.aLazyValue

        value shouldNotBe null
        value shouldBe s.aLazyValue
        s.clearLazyValue(Something::aLazyValue)
        value shouldNotBe s.aLazyValue
    }

    "lazy load object" {
        val s = SomethingElses.findById(somethingElseId)

        s?.something?.name shouldBe "FirstValue"
    }

    "preload loads items" {
        val items = listOf(SomethingElses.findById(somethingElseId)!!)

        items.preloadSomething().preloadSomethings().preloadSomethingNull()

        val lazyLoads = items.first().lazyEntityValues()
        lazyLoads.size shouldBe 3
        lazyLoads[SomethingElse::something.name] shouldNotBe null
        lazyLoads[SomethingElse::somethings.name] shouldNotBe null
        lazyLoads[SomethingElse::somethingNull.name] shouldBe null
    }

    "populate from map" {
        val data = mapOf(
            "num" to 1,
            "name" to "myName",
            "date" to "2001-01-01",
            "dateTime" to "2001-01-01T01:00:00",
            "enum" to "One",
            "long" to 1
        )

        val entity = Something {}
        entity.populateFrom(data, SomethingInfo::class)

        entity.num shouldBe 1
        entity.long shouldBe 1L
        entity.name shouldBe "myName"
        entity.enum shouldBe MyEnum.One
        entity.date shouldBe LocalDate.of(2001, 1, 1)
        entity.dateTime shouldBe LocalDateTime.of(2001, 1, 1, 1, 0)
    }

    "validation throws exception when invalid" {
        val s = Something { name = "" }

        shouldThrow<ValidationException> {
            s.validate()
        }
    }

    "validation does nothing when valid" {
        val s = Something { name = "blah" }

        s.validate()
    }

    "does not load lazy value if not loaded" {
        val items = listOf(SomethingElses.findById(somethingElseId)!!)
        items.preloadSomething()

        items.first().something.name shouldBe "FirstValue"

        items.first().something.name = "AnotherValue"
        items.preloadSomething()

        items.first().something.name shouldBe "AnotherValue"

        items.clearLazyValues().preloadSomething()
        items.first().something.name shouldBe "FirstValue"
    }

    "cannot call new repo method without mocking" {
        shouldThrow<IllegalStateException> {
            Somethings.findByLotsOfThings("blah", 2, now())
        }
    }

    "can mock new repo method" {
        every { Somethings.create(any()) } returns Something { name = "mocked" }

        val a = Somethings.create("anything")

        a.name shouldBe "mocked"
    }
})
