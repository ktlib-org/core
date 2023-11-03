package org.ktlib.entities

import org.ktlib.lazyConfigList
import java.util.*
import kotlin.reflect.KProperty
import kotlin.reflect.full.instanceParameter

class UnauthorizedException(message: String? = null) : RuntimeException(message)
class NotFoundException(message: String? = null) : RuntimeException(message)

data class ValidationError(val field: String, val message: String)

data class ValidationErrors(val errors: MutableList<ValidationError> = mutableListOf()) {
    val isNotEmpty: Boolean
        get() = errors.isNotEmpty()

    val isEmpty: Boolean
        get() = errors.isEmpty()

    val size: Int
        get() = errors.size
}

class ValidationException(val validationErrors: ValidationErrors) : RuntimeException(validationErrors.toString())

object Validation {
    private val validEmailDomains by lazyConfigList<String>("email.validUserDomains", listOf())
    private val errorHolder = ThreadLocal<ValidationErrors?>()
    private val fieldHolder = ThreadLocal<KProperty<*>?>()
    private val fieldValueHolder = ThreadLocal<Any?>()
    private val entityHolder = ThreadLocal<Any?>()

    fun validate(block: () -> Any?) {
        val errors = doValidate(block)
        if (errors.isNotEmpty) throw ValidationException(errors)
    }

    private fun doValidate(block: () -> Any?): ValidationErrors {
        val errors = ValidationErrors()
        errorHolder.set(errors)
        block()
        fieldHolder.set(null)
        fieldValueHolder.set(null)
        errorHolder.set(null)
        return errors
    }

    fun <E : Any?> E.validate(block: () -> Any?): E {
        entityHolder.set(this)
        val errors = doValidate(block)
        entityHolder.set(null)
        if (errors.isNotEmpty) throw ValidationException(errors)
        return this
    }

    fun validateField(prop: KProperty<*>, value: Any?, block: () -> Any?) {
        validate {
            field(prop, value, block)
        }
    }

    fun field(prop: KProperty<*>, value: Any? = Unit, block: () -> Any?) {
        fieldHolder.set(prop)
        if (value == Unit) {
            val entity = entityHolder.get()
                ?: throw IllegalArgumentException("You must provide a field value to 'field' or an Entity to 'validate'")
            prop.instanceParameter
            val fieldValue = if (prop.parameters.isEmpty()) prop.getter.call() else prop.getter.call(entity)
            fieldValueHolder.set(fieldValue)
        } else {
            fieldValueHolder.set(value)
        }
        block()
        fieldHolder.set(null)
        fieldValueHolder.set(null)
    }

    private fun checkErrorHolder(): ValidationErrors {
        val errors = errorHolder.get()
        if (errors == null) {
            throw IllegalStateException("You must wrap validations in a 'validate' block")
        } else {
            return errors
        }
    }

    fun error(message: String) {
        checkErrorHolder().errors.add(ValidationError(checkFieldName(), message))
    }

    private fun checkFieldName(): String {
        val field = fieldHolder.get()
        if (field == null) {
            throw IllegalStateException("You must wrap field validations in a 'field' block")
        } else {
            return field.name
        }
    }

    fun notNull(message: String = "${checkFieldName()} cannot be null") {
        if (fieldValueHolder.get() == null) error(message)
    }

    fun notBlank(message: String = "${checkFieldName()} cannot be blank") {
        when (val value = fieldValueHolder.get()) {
            null -> error(message)
            is String -> if (value.isBlank()) error(message)
            else -> throw IllegalArgumentException("You can only check that strings are not blank")
        }
    }

    fun lengthAtLeast(
        length: Int,
        message: String = "${checkFieldName()} must be at least $length characters in length"
    ) {
        when (val value = fieldValueHolder.get()) {
            null -> notNull()
            is String -> if (value.length < length) error(message)
            else -> throw IllegalArgumentException("You can only check the length of a string")
        }
    }

    fun check(message: String, checker: () -> Boolean) {
        if (!checker()) error(message)
    }

    fun validEmailDomain() {
        when (val value = fieldValueHolder.get()) {
            null -> notNull()
            is String -> {
                if (validEmailDomains.isNotEmpty() && validEmailDomains.none { value.lowercase().endsWith("@$it") }) {
                    error("Invalid email address domain")
                }
            }

            else -> throw IllegalArgumentException("You can only check valid email domain of a string")
        }
    }

    fun validId(block: (id: UUID) -> Boolean) {
        val id = fieldValueHolder.get() as UUID?
        check("Invalid ID") {
            id == null || block(id)
        }
    }
}