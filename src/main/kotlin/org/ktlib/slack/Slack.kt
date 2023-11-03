package org.ktlib.slack

import org.ktlib.*
import org.ktlib.error.ErrorReporter

@SnakeCaseJson
data class AppMessage(val text: String, val channel: String? = null, val blocks: List<MessageBlock>? = null)
enum class MessageBlockType { section, context, header, divider, actions }
data class PostResponse(val ok: Boolean)
data class MessageBlock(
    val type: MessageBlockType,
    val text: TextElement? = null,
    val accessory: Accessory? = null,
    val elements: List<Element>? = null
)

enum class TextType { mrkdwn, plain_text }
data class TextElement(val type: TextType, val text: String, val emoji: Boolean = false)
data class Accessory(val type: AccessoryType, val imageUrl: String, val altText: String? = null)
enum class AccessoryType { image }
enum class ElementType { button, image }
data class Element(
    val type: ElementType,
    val text: TextElement,
    val actionId: String? = null,
    val url: String? = null,
    val imageUrl: String? = null,
    val altText: String? = null,
)

@SnakeCaseJson
data class AccessTokenResponse(var accessToken: String, var teamName: String)

@SnakeCaseJson
data class WebhookMessage(
    val text: String,
    val channel: String? = null,
    val iconEmoji: String? = null,
    val iconUrl: String? = null,
    val username: String? = null,
    val attachments: List<Attachment>? = null,
)

data class Attachment(
    val fallback: String,
    val text: String? = null,
    val pretext: String? = null,
    val color: String? = null,
    val fields: List<Field>? = null,
)

data class Field(val title: String, val value: String, val short: Boolean = false)

object Slack {
    private val defaultWebhookUrl: String? = configOrNull("slack.webhookUrl")
    private val clientId by lazyConfig<String>("slack.clientId")
    private val clientSecret by lazyConfig<String>("slack.clientSecret")
    private const val apiUrl = "https://slack.com/api"

    fun getToken(code: String, redirectUri: String): AccessTokenResponse {
        val response =
            "$apiUrl/oauth.access".httpPostForm<String>(
                mapOf(
                    "client_id" to clientId,
                    "client_secret" to clientSecret,
                    "code" to code,
                    "redirect_uri" to redirectUri
                )
            )

        try {
            return response.fromJson()
        } catch (e: Exception) {
            ErrorReporter.report("Error getting slack token", mapOf("response" to response))
            throw e
        }
    }

    fun sendMessage(token: String, message: AppMessage) {
        val response = "$apiUrl/chat.postMessage".httpPost<String>(message, mapOf("Authorization" to "Bearer $token"))

        val postMessageResponse = response.fromJson<PostResponse>()

        if (!postMessageResponse.ok) {
            ErrorReporter.report("Slack message failed to send", mapOf("response" to response, "message" to message))
        }
    }

    fun sendMessage(message: WebhookMessage) {
        if (defaultWebhookUrl != null) {
            sendMessage(defaultWebhookUrl, message)
        }
    }

    fun sendMessage(webHookUrl: String, message: WebhookMessage) {
        val response = webHookUrl.httpPost<String>(message)

        if (response != "ok") {
            ErrorReporter.report("Error sending Slack message", mapOf("response" to response, "message" to message))
        }
    }
}