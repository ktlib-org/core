package org.ktlib

import org.springframework.security.crypto.bcrypt.BCrypt
import java.security.MessageDigest
import java.util.*

/**
 * Hashes the string using Spring Security BCrypt.hashpw().
 */
fun String.hashPassword(): String = Encryption.hashPassword(this)

/**
 * Tests if the unhashed password in the String matches the specified hashed password using Spring Security BCrypt.checkpw().
 * @param hashedPassword the hashed password to check the string against
 * @return true if password matches
 */
fun String.matchesHashedPassword(hashedPassword: String) = Encryption.passwordMatches(this, hashedPassword)

/**
 * @return the SHA-512 hash of the String
 */
fun String.sha512(): String = Encryption.hashString("SHA-512", this)

/**
 * @return the SHA-256 hash of the String
 */
fun String.sha256(): String = Encryption.hashString("SHA-256", this)

/**
 * @return the SHA-1 hash of the String
 */
fun String.sha1(): String = Encryption.hashString("SHA-1", this)

/**
 * @return a String containing the Base64 encoding of the ByteArray
 */
fun ByteArray.base64Encode(): String = Base64.getEncoder().encodeToString(this)

/**
 * @return a ByteArray containing the Base64 encoding of the ByteArray
 */
fun ByteArray.base64EncodeAsBytes(): ByteArray = Base64.getEncoder().encode(this)

/**
 * @return a ByteArray from Base64 decoding the string
 */
fun String.base64Decode(): ByteArray = Base64.getDecoder().decode(this)

/**
 * @return a ByteArray from Base64 decoding the string
 */
fun ByteArray.base64Decode(): ByteArray = Base64.getDecoder().decode(this)

/**
 * Generates a random string with numbers and letters of the given length.
 * @param length the length of the string
 * @return random string of given length
 */
fun generateKey(length: Int) = Encryption.generateKey(length)

/**
 * Holds some helpful Encryption type function.
 */
object Encryption {
    private val keyReplace = "[+=/]".toRegex()

    fun hashPassword(password: String): String = BCrypt.hashpw(password, BCrypt.gensalt())

    fun passwordMatches(password: String, hashedPassword: String) = BCrypt.checkpw(password, hashedPassword)

    fun hashString(type: String, input: String) =
        MessageDigest.getInstance(type).digest(input.toByteArray()).base64Encode()

    fun generateKey(length: Int): String {
        if (length < 1) return ""
        val key = UUID.randomUUID().toString().sha512().replace(keyReplace, "").take(length)
        return key + generateKey(length - key.length)
    }
}