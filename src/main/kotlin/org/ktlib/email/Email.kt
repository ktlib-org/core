package org.ktlib.email

import org.ktlib.config
import org.ktlib.lookup

data class EmailData(val email: String, val name: String? = null)

interface Email {
    companion object : Email by lookup()

    val defaultFromAddress: EmailData
        get() = EmailData(config("email.defaultFrom"), config("email.defaultFromName"))

    fun send(
        template: String,
        to: EmailData,
        data: Any? = null,
        from: EmailData = defaultFromAddress,
        subject: String? = null,
        replyTo: EmailData? = null
    )
}