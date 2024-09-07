package org.ktlib.files

import org.ktlib.lookupInstance
import java.io.InputStream
import java.nio.file.Path
import java.time.Duration

enum class FileMethod { GET, PUT }
data class FileInfo(val contentType: String, val contentLength: Long, val inputStream: InputStream)

/**
 * Interface defining basic interaction with a file storage system.
 */
interface FileStorage {
    companion object : FileStorage by lookupInstance()

    fun save(basePath: String, path: String, content: String)
    fun save(basePath: String, path: String, localPath: Path)
    fun save(basePath: String, path: String, inputStream: InputStream, contentLength: Long, contentType: String)
    fun exists(basePath: String, path: String): Boolean
    fun dirExists(basePath: String, path: String): Boolean
    fun delete(basePath: String, path: String)
    fun list(basePath: String, path: String): List<String>
    fun load(basePath: String, path: String): InputStream
    fun loadWithType(basePath: String, path: String): FileInfo
    fun createUrl(
        basePath: String,
        path: String,
        method: FileMethod,
        validFor: Duration = Duration.ofMinutes(10)
    ): String
}