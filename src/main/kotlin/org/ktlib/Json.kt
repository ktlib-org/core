package org.ktlib

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.PropertyNamingStrategies.*
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.cfg.MapperBuilder
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.type.TypeFactory
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.io.*
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.findAnnotation

/**
 * This annotation tells the JSON parser to use kebab case when converting to/from a JSON string.
 */
@Target(AnnotationTarget.CLASS)
@Retention
annotation class KebabCaseJson

/**
 * This annotation tells the JSON parser to use snake case when converting to/from a JSON string.
 */
@Target(AnnotationTarget.CLASS)
@Retention
annotation class SnakeCaseJson

/**
 * This annotation tells the JSON parser to use upper camelcase when converting to/from a JSON string.
 */
@Target(AnnotationTarget.CLASS)
@Retention
annotation class UpperCamelCaseJson

typealias TypeRef<T> = TypeReference<T>

/**
 * Creates a type ref
 */
inline fun <reified T> typeRef(): TypeRef<T> = object : TypeRef<T>() {}

/**
 * Deserializes the JSON string into the specified type.
 */
fun <T> String.fromJson(type: TypeRef<T>): T = Json.deserialize(this, type)

/**
 * Deserializes the JSON string into the specified type.
 */
fun <T : Any> String.fromJson(type: KClass<T>): T = Json.deserialize(this, type)

/**
 * Deserializes the JSON string into the specified type.
 */
fun <T : Any> ByteArray.fromJson(type: KClass<T>): T = Json.deserialize(this, type)

/**
 * Converts the object into a JSON string.
 */
fun Any.toJson(): String = Json.serialize(this)

/**
 * Converts the object into a JSON byte array.
 */
fun Any.toJsonBytes(): ByteArray = Json.serializeAsBytes(this)

/**
 * Deserializes the JSON string into an object.
 */
inline fun <reified T> String.fromJson(): T = Json.deserialize(this, typeRef())

/**
 * Deserializes a JSON ByteArray into an object.
 */
inline fun <reified T> ByteArray.fromJson(): T = Json.deserialize(this, typeRef())

/**
 * This object wraps all the JSON functions commonly used.
 */
object Json {
    val camelCaseMapper = createJsonMapper(LOWER_CAMEL_CASE)
    val kebabCaseMapper = createJsonMapper(KEBAB_CASE)
    val snakeCaseMapper = createJsonMapper(SNAKE_CASE)
    val upperCamelCaseMapper = createJsonMapper(UPPER_CAMEL_CASE)

    private fun createJsonMapper(namingStrategy: PropertyNamingStrategy) = JsonMapper.builder()
        .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
        .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
        .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .enable(JsonReadFeature.ALLOW_MISSING_VALUES)
        .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
        .addModule(Jdk8Module())
        .addModule(JavaTimeModule())
        .addModule(KotlinModule.Builder().build())
        .propertyNamingStrategy(namingStrategy)
        .apply { addModuleIfAvailable("org.ktorm.jackson.KtormModule", this) }
        .build()

    private fun addModuleIfAvailable(className: String, builder: MapperBuilder<*, *>) {
        try {
            builder.addModule(Class.forName(className).kotlin.createInstance() as Module)
        } catch (_: ClassNotFoundException) {
        }
    }


    fun <T> deserialize(value: ByteArray, ref: TypeRef<T>) = mapper(ref).readValue(value, ref)

    fun <T : Any> deserialize(value: ByteArray, ref: KClass<T>) = mapper(ref).readValue(value, ref.java)

    fun <T> deserialize(value: String, ref: TypeRef<T>) = mapper(ref).readValue(value, ref)

    fun <T : Any> deserialize(value: String, ref: KClass<T>) = mapper(ref).readValue(value, ref.java)

    fun <T> deserialize(input: InputStream, ref: TypeRef<T>) = mapper(ref).readValue(input, ref)

    fun <T : Any> deserialize(value: InputStream, ref: KClass<T>) = mapper(ref).readValue(value, ref.java)

    fun <T> deserialize(input: Reader, ref: TypeRef<T>) = mapper(ref).readValue(input, ref)

    fun <T : Any> deserialize(value: Reader, ref: KClass<T>) = mapper(ref).readValue(value, ref.java)

    fun <T> deserialize(input: File, ref: TypeRef<T>) = mapper(ref).readValue(input, ref)

    fun <T : Any> deserialize(value: File, ref: KClass<T>) = mapper(ref).readValue(value, ref.java)

    fun serializeAsBytes(value: Any) = mapper(value::class).writeValueAsBytes(value)

    fun serialize(value: Any) = mapper(value::class).writeValueAsString(value)

    fun serialize(value: Any, output: OutputStream) = mapper(value::class).writeValue(output, value)

    fun serialize(value: Any, output: Writer) = mapper(value::class).writeValue(output, value)

    fun serialize(value: Any, output: File) = mapper(value::class).writeValue(output, value)

    private fun <T> mapper(ref: TypeRef<T>): JsonMapper {
        val typeName = ref.type.typeName
        return when {
            isTypeWithUnknownContents(typeName) -> camelCaseMapper
            isGenericList(typeName) -> mapper(ref.javaClass.classLoader.loadClass(getListType(typeName)).kotlin)
            else -> mapper(TypeFactory.rawClass(ref.type).kotlin)
        }
    }

    private fun isTypeWithUnknownContents(typeName: String) =
        typeName.startsWith("java.util.Map") || typeName == "java.util.List<?>"

    private fun isGenericList(typeName: String) =
        typeName.startsWith("java.util.List") && typeName.contains(" extends ")

    private fun getListType(typeName: String?) = typeName?.substringAfter(" extends ")?.dropLast(1)?.trim()

    private fun <T : Any> mapper(type: KClass<T>) = when {
        type.findAnnotation<KebabCaseJson>() != null -> kebabCaseMapper
        type.findAnnotation<SnakeCaseJson>() != null -> snakeCaseMapper
        type.findAnnotation<UpperCamelCaseJson>() != null -> upperCamelCaseMapper
        else -> camelCaseMapper
    }
}
