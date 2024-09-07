package org.ktlib

import com.github.f4b6a3.uuid.UuidCreator
import com.github.f4b6a3.uuid.codec.base.Base16Codec
import org.ktlib.error.ErrorReporter
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.Files
import java.time.*
import java.time.temporal.ChronoField
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.math.round
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

internal object Util {
    val classLoader: ClassLoader = Util.javaClass.classLoader
}

fun KFunction<*>.toQualifiedName() = toString().substringAfter(" ").substringBefore("(")

/**
 * This is a convenience class that adds an init function that can be called to
 * force the init block of the class to execute.
 *
 * @param blocks list of blocks of code that will be executed sequentially as part of the init
 */
open class Init(vararg blocks: () -> Any?) {
    init {
        blocks.forEach { it() }
    }

    /**
     * Ensures the init block on the object is run
     */
    fun init() = Unit
}

// String Functions
/**
 * @return the contents of the resource path in the string or null if it doesn't exist
 */
fun String.resourceAsString() = Util.classLoader.getResource(this)?.readText()

/**
 * @return true if a classpath resource exists with the path in the string
 */
fun String.resourceExists() = Util.classLoader.getResource(this) != null

/**
 * @return the URL encoded string
 */
fun String.urlEncode(): String = URLEncoder.encode(this, "UTF-8")

// Gzip functions
fun String.gzip(): String = ByteArrayOutputStream().use { bos ->
    GZIPOutputStream(bos).bufferedWriter(StandardCharsets.UTF_8).use { it.write(this) }
    bos.toByteArray().base64Encode()
}

fun String.ungzip(): String =
    GZIPInputStream(this.base64Decode().inputStream()).bufferedReader(StandardCharsets.UTF_8).use { it.readText() }

fun String.toUUID(): UUID =
    if (this.length == 32) {
        Base16Codec.INSTANCE.decode(this)
    } else {
        UUID.fromString(this)
    }

fun newUUID7(): UUID = UuidCreator.getTimeOrderedEpoch()

fun newUUID4(): UUID = UuidCreator.getRandomBasedFast()

fun UUID.toHexString(): String = Base16Codec.INSTANCE.encode(this)

fun String.decodeHex(): ByteArray {
    check(length % 2 == 0) { "Must have an even length" }

    return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}

fun ByteArray.gzip(): ByteArray = ByteArrayOutputStream().use { bos ->
    GZIPOutputStream(bos).use { it.write(this) }
    bos.toByteArray()
}

fun ByteArray.ungzip(): ByteArray =
    GZIPInputStream(this.inputStream()).use { it.readBytes() }

// Date Functions
fun now(): LocalDateTime = LocalDateTime.now()
fun now(zone: ZoneId): ZonedDateTime = ZonedDateTime.now(zone)
fun today(): LocalDate = LocalDate.now()
fun today(zone: ZoneId): LocalDate = LocalDate.now(zone)
fun nowMillis() = System.currentTimeMillis()

fun Int.minutesAgo(): LocalDateTime = now().minusMinutes(this.toLong())
fun Int.minutesFromNow(): LocalDateTime = now().plusMinutes(this.toLong())
fun Int.hoursAgo(): LocalDateTime = now().minusHours(this.toLong())
fun Int.hoursFromNow(): LocalDateTime = now().plusHours(this.toLong())
fun Int.daysAgo(): LocalDate = today().minusDays(this.toLong())
fun Int.daysFromNow(): LocalDate = today().plusDays(this.toLong())

fun Int.secondsInMillis() = this * 1000L
fun Int.minutesInMillis() = this * 60 * 1000L
fun Int.hoursInMillis() = this * 60 * 60 * 1000L

fun Long.toLocalDateTimeFromMillis(zone: ZoneId = ZoneOffset.UTC): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(this), zone)

fun LocalDateTime.millisSince(other: LocalDateTime) = this.toMillisUtc() - other.toMillisUtc()
fun LocalDateTime.secondsSince(other: LocalDateTime) = round(this.millisSince(other) / 1000.0).toLong()
fun LocalDateTime.millisUntil(other: LocalDateTime) = -this.millisSince(other)
fun LocalDateTime.secondsUntil(other: LocalDateTime) = -this.secondsSince(other)
fun LocalDateTime.toMillisUtc() = this.toEpochSecond(ZoneOffset.UTC) * 1000 + this.get(ChronoField.MILLI_OF_SECOND)

fun LocalDateTime.toStartOfMinute(): LocalDateTime = LocalDateTime.of(year, month, dayOfMonth, hour, minute)
fun LocalDateTime.toStartOfHour(): LocalDateTime = LocalDateTime.of(year, month, dayOfMonth, hour, 0)
fun LocalDateTime.toStartOfDay(): LocalDateTime = LocalDateTime.of(year, month, dayOfMonth, 0, 0)
fun LocalDateTime.toStartOfMonth(): LocalDateTime = LocalDateTime.of(year, month, 1, 0, 0)


// Error reporting

/**
 * Catches any Throwable that comes out of the block and sends it to the ErrorReporter, then rethrows the Throwable
 *
 * @param block the block of code to execute
 */
fun <T> reportAndThrow(block: () -> T?): T? = reportAndThrow({ null }, block)

/**
 * Catches any Throwable that comes out of the block and sends it to the ErrorReporter, then rethrows the Throwable
 *
 * @param addedInfo callback that can add info to send to the ErrorReporter
 * @param block the block of code to execute
 */
fun <T> reportAndThrow(addedInfo: (t: Throwable) -> Map<String, Any>?, block: () -> T?) =
    try {
        block()
    } catch (t: Throwable) {
        ErrorReporter.report(t, addedInfo(t))
        throw t
    }

/**
 * Catches any Throwable that comes out of the block and sends it to the ErrorReporter, then returns null
 *
 * @param block the block of code to execute
 */
fun <T> reportAndSwallow(block: () -> T?): T? = reportAndSwallow({ null }, block)

/**
 * Catches any Throwable that comes out of the block and sends it to the ErrorReporter, then returns null
 *
 * @param addedInfo callback that can add info to send to the ErrorReporter
 * @param block the block of code to execute
 */
fun <T> reportAndSwallow(addedInfo: (t: Throwable) -> Map<String, Any>?, block: () -> T?) =
    try {
        block()
    } catch (t: Throwable) {
        ErrorReporter.report(t, addedInfo(t))
        null
    }

/**
 * This function will find all the classes in the classpath that are in the same package or below
 * as the class of the first type parameter and are a subclass of the second type parameter. It will then
 * create an instances of each class and return them in a list. The classes must have a no-arg constructor
 * or be an object.
 *
 * @return a list of instances of all the classes found
 */
inline fun <reified T : Any, reified R : Any> instancesFromFilesRelativeToClass() =
    classesFromFilesRelativeToClass(T::class)
        .filter { it.isSubclassOf(R::class) && !it.isAbstract }
        .map { it.objectInstance ?: it.createInstance() }
        .filterIsInstance<R>()

/**
 * This function will find all the classes in the classpath that are in the same package or below
 * as the class of the type parameter and return the KClass for each class.
 *
 * @return a list of KClass for all the classes found
 */
inline fun <reified T> classesFromFilesRelativeToClass() = classesFromFilesRelativeToClass(T::class)

/**
 * This function will find all the classes in the classpath that are in the same package or below
 * as the class passed in and return the KClass for each class.
 *
 * @param kClass the class to use to find the package to search
 * @return a list of KClass for all the classes found
 */
fun classesFromFilesRelativeToClass(kClass: KClass<*>): List<KClass<*>> {
    val resourcePath = "/${kClass.qualifiedName?.replace(".", "/")}.class"
    val resource = kClass.java.getResource(resourcePath)!!

    val (directory, base) = if (resource.protocol == "jar") {
        val jarFile = File(resource.toString().substringBefore("!").substringAfterLast(":")).toPath()
        val fileSystem = FileSystems.newFileSystem(jarFile, emptyMap<String, Any>())
        val internalPath = resource.toString().substringAfter("!").substringBeforeLast("/") + "/"
        val dir = fileSystem.getPath(internalPath)
        Pair(dir, "/")
    } else {
        val dir = File(resource.file).parentFile
        val b = dir.canonicalPath.substringBefore(resourcePath.substringBeforeLast("/")) + "/"
        Pair(dir.toPath(), b)
    }

    return Files.walk(directory)
        .filter {
            val path = it.toString()
            !path.contains("$") && path.endsWith(".class") && !path.endsWith("Kt.class")
        }
        .map { it.toString().removePrefix(base).removeSuffix(".class").replace('/', '.') }
        .map { Class.forName(it).kotlin }
        .toList()
}

fun Any.typeArguments(forType: KClass<*>): List<KClass<*>> = typeArguments(this::class, forType)

fun typeArguments(ofType: KClass<*>, forType: KClass<*>): List<KClass<*>> {
    val matchingSuperclass = ofType.supertypes.find { it == forType || it.jvmErasure.isSubclassOf(forType) }
    val arguments = matchingSuperclass?.arguments
    return when {
        arguments == null -> throw Exception("$forType is not a supertype of $ofType")
        arguments.isEmpty() -> typeArguments(matchingSuperclass.jvmErasure, forType)
        else -> arguments.map { it.type!!.jvmErasure }
    }
}
