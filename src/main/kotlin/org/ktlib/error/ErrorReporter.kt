package org.ktlib.error

import org.ktlib.lookupInstance

/**
 * Interface that defines an error reporter.
 */
interface ErrorReporter {
    companion object : ErrorReporter by lookupInstance(ConsoleErrorReporter)

    fun report(t: Throwable, additionalInfo: Map<String, Any>? = null)
    fun report(message: String, additionalInfo: Map<String, Any>? = null)
    fun setContext(infoContext: Any)
    fun setUserInfo(userId: String, email: String? = null, ipAddress: String? = null)
    fun addBreadcrumb(message: String)
    fun clearBreadcrumbs()
    fun addContext(key: String, value: Any)
    fun addContext(values: Map<String, Any>?)
    fun removeContext(keys: Collection<String>?)
    fun removeContext(vararg keys: String)
}