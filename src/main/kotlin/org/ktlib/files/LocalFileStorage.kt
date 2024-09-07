package org.ktlib.files

import org.ktlib.Application
import java.io.File
import java.io.InputStream
import java.nio.file.Path
import java.time.Duration

/**
 * Implementation of FileStorage that uses local file system.
 */
object LocalFileStorage : FileStorage {
    private val localFileDir = File(System.getProperty("user.home"), "." + Application.name)

    init {
        if (!localFileDir.exists()) {
            localFileDir.mkdirs()
        }
    }

    private fun file(basePath: String, path: String) = File(File(localFileDir, basePath), path)

    override fun save(basePath: String, path: String, content: String) {
        file(basePath, path).writeText(content)
    }

    override fun save(basePath: String, path: String, localPath: Path) {
        file(basePath, path).writeBytes(localPath.toFile().readBytes())
    }

    override fun save(
        basePath: String,
        path: String,
        inputStream: InputStream,
        contentLength: Long,
        contentType: String
    ) {
        file(basePath, path).writeBytes(inputStream.readBytes())
    }

    override fun exists(basePath: String, path: String) = file(basePath, path).exists()

    override fun dirExists(basePath: String, path: String): Boolean {
        val file = file(basePath, path)
        return file.exists() && file.isDirectory
    }

    override fun delete(basePath: String, path: String) {
        file(basePath, path).delete()
    }

    override fun list(basePath: String, path: String): List<String> {
        return file(basePath, path).list()?.toList() ?: listOf()
    }

    override fun load(basePath: String, path: String): InputStream {
        return file(basePath, path).inputStream()
    }

    override fun loadWithType(basePath: String, path: String): FileInfo {
        val file = file(basePath, path)
        return FileInfo(file.name.substringAfterLast("."), file.readBytes().size.toLong(), file.inputStream())
    }

    override fun createUrl(basePath: String, path: String, method: FileMethod, validFor: Duration): String {
        throw UnsupportedOperationException("Cannot get file url for local storage")
    }
}