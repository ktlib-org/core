package org.ktlib.email

import org.ktlib.SnakeCaseJson
import org.ktlib.lazyConfig
import org.ktlib.postToUrl

/**
 * Email implementation using SendGrid. To see the SendGrid documentation visit https://sendgrid.com/docs/API_Reference/api_v3.html
 */
object SendGrid : Email {
    private const val URL = "https://api.sendgrid.com/v3/mail/send"
    private val apiKey by lazyConfig<String>("email.sendgrid.apiKey")

    @SnakeCaseJson
    private data class Data(
        val personalizations: List<Personalization>,
        val from: EmailData,
        val replyTo: EmailData?,
        val templateId: String
    )

    @SnakeCaseJson
    private data class Personalization(val to: List<EmailData>, val subject: String?, val dynamicTemplateData: Any?)

    override fun send(
        template: String,
        to: EmailData,
        data: Any?,
        from: EmailData,
        subject: String?,
        replyTo: EmailData?
    ) {
        Data(
            listOf(Personalization(listOf(to), subject, data)),
            from,
            replyTo,
            template
        ).postToUrl<Unit>(URL, mapOf("Authorization" to "Bearer $apiKey"))
    }
}