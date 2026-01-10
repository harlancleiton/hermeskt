package br.com.olympus.hermes.shared.domain.exceptions

/**
 * Base sealed class for all application exceptions in the Hermes system.
 * 
 * This sealed class provides a common foundation for exception handling by categorizing
 * exceptions into client errors (4xx) and server errors (5xx). The sealed nature ensures
 * that all exceptions extend from either ClientException or ServerException.
 *
 * @param message The detail message for this exception
 * @param cause The cause of this exception (can be null)
 */
sealed class BaseException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /**
     * Determines if this exception represents a client error.
     * Client errors typically indicate problems with the request made by the client.
     *
     * @return true if this is a client error, false otherwise
     */
    abstract fun isClientError(): Boolean

    /**
     * Determines if this exception represents a server error.
     * Server errors typically indicate problems on the server side.
     *
     * @return true if this is a server error, false otherwise
     */
    abstract fun isServerError(): Boolean

    /**
     * Abstract base class for client-side exceptions (typically 4xx HTTP status codes).
     * 
     * Client exceptions indicate that the error is due to something the client did,
     * such as providing invalid data, unauthorized access, or requesting a non-existent resource.
     *
     * @param message The detail message for this client exception
     * @param cause The cause of this exception (can be null)
     */
    abstract class ClientException(message: String, cause: Throwable? = null) : BaseException(message, cause) {

        /**
         * Always returns true for client exceptions.
         *
         * @return true indicating this is a client error
         */
        override fun isClientError() = true

        /**
         * Always returns false for client exceptions.
         *
         * @return false indicating this is not a server error
         */
        override fun isServerError() = false
    }

    /**
     * Abstract base class for server-side exceptions (typically 5xx HTTP status codes).
     * 
     * Server exceptions indicate that the error occurred due to a server-side issue,
     * such as internal server errors, service unavailability, or database connection problems.
     *
     * @param message The detail message for this server exception
     * @param cause The cause of this exception (can be null)
     */
    abstract class ServerException(message: String, cause: Throwable? = null) : BaseException(message, cause) {

        /**
         * Always returns false for server exceptions.
         *
         * @return false indicating this is not a client error
         */
        override fun isClientError() = false

        /**
         * Always returns true for server exceptions.
         *
         * @return true indicating this is a server error
         */
        override fun isServerError() = true
    }
}
