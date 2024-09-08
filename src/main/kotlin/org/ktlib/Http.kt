package org.ktlib

import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.ktlib.trace.Trace
import java.io.File
import java.io.IOException

typealias Params = Map<String, Any>
typealias Headers = Map<String, String>

data class Response<T>(val data: T, val headers: Map<String, List<String>>)

/**
 * Does an HTTP GET call to the URL in the String.
 * @param params map of URL params for the request
 * @param headers map of headers for the request
 * @return response parsed to the specified type
 */
inline fun <reified T : Any> String.httpGet(params: Params = mapOf(), headers: Headers = mapOf()): T =
    Http.get(this, typeRef(), params, headers)

/**
 * Does an HTTP GET call to the URL in the String.
 * @param params map of URL params for the request
 * @param headers map of headers for the request
 * @return Response object with the data parsed into the specified type and the response headers
 */
inline fun <reified T : Any> String.httpGetResponse(params: Params = mapOf(), headers: Headers = mapOf()): Response<T> =
    Http.getResponse(this, typeRef(), params, headers)

/**
 * Does an HTTP POST as form data to the URL
 * @param data param data that is posted to the URL as form params
 * @param headers map of headers for the request
 * @return response parsed to the specified type
 */
inline fun <reified T : Any> String.httpPostForm(data: Params, headers: Headers = mapOf()): T =
    Http.postForm(this, data, typeRef(), headers)

/**
 * Does an HTTP POST call to the URL in the String.
 * @param data data that will be serialized to JSON and sent to the URL
 * @param headers map of headers for the request
 * @return response parsed to the specified type
 */
inline fun <reified T : Any> String.httpPost(data: Any, headers: Headers = mapOf()): T =
    Http.post(this, data, typeRef(), headers)

/**
 * Serializes the object to JSON and posts it to the specified URL.
 * @param url the URL to post the data to
 * @param headers map of headers for the request
 * @return response parsed to the specified type
 */
inline fun <reified T : Any> Any.postToUrl(url: String, headers: Headers = mapOf()): T =
    Http.post(url, this, typeRef(), headers)

/**
 * Does an HTTP PUT call to the URL in the String.
 * @param data data that will be serialized to JSON and sent to the URL
 * @param headers map of headers for the request
 * @return response parsed to the specified type
 */
inline fun <reified T : Any> String.httpPut(data: Any, headers: Headers = mapOf()): T =
    Http.put(this, data, typeRef(), headers)

/**
 * Does an HTTP PATCH call to the URL in the String.
 * @param data data that will be serialized to JSON and sent to the URL
 * @param headers map of headers for the request
 * @return response parsed to the specified type
 */
inline fun <reified T : Any> String.httpPatch(data: Any, headers: Headers = mapOf()): T =
    Http.patch(this, data, typeRef(), headers)

/**
 * Does an HTTP delete call to the URL in the String.
 * @param params map of URL params for the request
 * @param headers map of headers for the request
 */
inline fun <reified T : Any> String.httpDelete(params: Params = mapOf(), headers: Headers = mapOf()): Unit =
    Http.delete(this, params, headers)

/**
 * Downloads a file at the specified URL and puts it into the specified directory.
 * @param dir directory to place the downloaded file into
 * @param headers map of headers for the request
 * @return a File object pointing to the downloaded file
 */
fun String.httpDownload(dir: File, headers: Headers = mapOf()): File = Http.download(this, dir, headers)

/**
 * Object for doing basic HTTP requests. It assumes you'll be sending and receiving JSON.
 */
object Http {
    const val TRACE_NAME_HEADER = "trace-name"
    private val debugBody = config<Boolean>("http.debugBody", false)
    private val client = OkHttpClient()
    private val formMediaType = "application/x-www-form-urlencoded".toMediaType()
    private val jsonMediaType = "application/json".toMediaType()
    private const val maxCharsToLog = 1200

    private val logger = KotlinLogging.logger {}

    fun <T : Any> get(url: String, type: TypeRef<T>, params: Params = mapOf(), headers: Headers = mapOf()): T =
        send(url.addParams(params), "GET", null, headers).deserialize(type)

    fun <T : Any> getResponse(url: String, type: TypeRef<T>, params: Params = mapOf(), headers: Headers = mapOf()) =
        send(url.addParams(params), "GET", null, headers).let {
            Response(data = it.deserialize(type), headers = it.headers)
        }

    fun <T : Any> post(url: String, data: Any, type: TypeRef<T>, headers: Headers = mapOf()) =
        send(url, "POST", data, headers).deserialize(type)

    fun <T : Any> postForm(url: String, params: Params, type: TypeRef<T>, headers: Headers = mapOf()) =
        callUrl(url, "POST", params.toParamString(), headers, formMediaType).deserialize(type)

    fun <T : Any> put(url: String, data: Any, type: TypeRef<T>, headers: Headers = mapOf()) =
        send(url, "PUT", data, headers).deserialize(type)

    fun <T : Any> patch(url: String, data: Any, type: TypeRef<T>, headers: Headers = mapOf()) =
        send(url, "PATCH", data, headers).deserialize(type)

    fun delete(url: String, params: Params = mapOf(), headers: Headers = mapOf()) {
        send(url.addParams(params), "DELETE", null, headers)
    }

    fun download(url: String, headers: Headers = mapOf()) = requestBuilder(url, "GET", headers).let {
        logger.debug { "Downloading from $url" }
        client.newCall(it).execute().body!!.byteStream()
    }

    fun download(url: String, dir: File, headers: Headers = mapOf()) = download(url, headers).use { input ->
        val name = url.substringBefore("?").substringAfterLast("/").ifBlank { Math.random().toString() }
        val file = File(dir, name)
        logger.debug { "To file ${file.absolutePath}" }

        file.outputStream().use { output ->
            input.copyTo(output)
            file
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> Response<String>.deserialize(type: TypeRef<T>): T = when (type.type) {
        Unit::class, Unit::class.java -> Unit as T
        String::class, String::class.java -> data as T
        else -> Json.deserialize(data, type)
    }

    private fun String.addParams(params: Params) =
        if (params.isNotEmpty()) this + "?" + params.toParamString() else this

    private fun Params.toParamString() = map { (key, value) -> "$key=$value" }.joinToString("&")

    private fun send(url: String, method: String, data: Any?, headers: Headers): Response<String> {
        val body = when (data) {
            null -> null
            is String -> data
            else -> Json.serialize(data)
        }
        return callUrl(url, method, body, headers, jsonMediaType)
    }

    private fun callUrl(
        url: String,
        method: String,
        body: String?,
        headers: Headers,
        mediaType: MediaType
    ): Response<String> {
        try {
            val domain = url.substringAfter("://").substringBefore("/")
            val traceName = headers[TRACE_NAME_HEADER] ?: domain
            Trace.start(
                "HTTP",
                traceName,
                mapOf(Pair("method", method), Pair("url", url.substringBeforeLast("?")), Pair("domain", domain))
            )
            logger.info { "Sending request to: $url" }
            logger.debug {
                """
            HttpRequest {
                Url: $url
                Method: $method
                Media: $mediaType
                Body: ${processBody(body ?: "")}
            }
        """.trimIndent()
            }

            val request = requestBuilder(url, method, headers, body, mediaType)

            return client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""

                logger.info { "Got response from $url with code ${response.code}" }
                logger.debug {
                    """
                HttpResponse {
                    Url: $url
                    Media: ${response.headers["content-type"]}
                    Headers: ${response.headers.toMultimap()}
                    Status: ${response.code}
                    Body: ${processBody(responseBody)}
                }
            """.trimIndent()
                }

                if (response.isSuccessful) {
                    Response(responseBody, response.headers.toMultimap())
                } else {
                    throw IOException("Http request failed to $url with error code ${response.code} and response body $responseBody")
                }
            }
        } finally {
            Trace.end()
        }
    }

    private fun requestBuilder(
        url: String,
        method: String,
        headers: Headers,
        body: String? = null,
        mediaType: MediaType? = null
    ): Request {
        val builder = Request.Builder().url(url).method(method, body?.toRequestBody(mediaType))
        headers.forEach { (key, value) ->
            if (key != TRACE_NAME_HEADER) {
                builder.addHeader(key, value)
            }
        }
        return builder.build()
    }

    private fun processBody(value: String): String {
        return if (!debugBody) "<removed>"
        else if (value.length > maxCharsToLog) "${value.take(maxCharsToLog)}..."
        else value
    }
}