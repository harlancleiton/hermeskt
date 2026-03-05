package br.com.olympus.hermes.shared.domain.factories

/**
 * Sealed interface representing the raw input data for notification creation. Each subtype carries
 * channel-specific primitive fields that will be validated and converted into domain value objects
 * by the corresponding [NotificationFactory].
 */
sealed interface CreateNotificationInput {
    val id: String
    val content: String
    val payload: Map<String, Any>

    /**
     * Raw input data for creating an email notification.
     *
     * @property content The email body content (validated for non-blank).
     * @property payload Additional metadata for template rendering.
     * @property from The sender's email address (raw string, validated by the factory).
     * @property to The recipient's email address (raw string, validated by the factory).
     * @property subject The email subject line (raw string, validated by the factory).
     */
    data class Email(
        override val id: String,
        override val content: String,
        override val payload: Map<String, Any> = emptyMap(),
        val from: String,
        val to: String,
        val subject: String,
    ) : CreateNotificationInput

    /**
     * Raw input data for creating an SMS notification.
     *
     * @property content The SMS body content (validated for non-blank).
     * @property payload Additional metadata for template rendering.
     * @property from The sender's short code.
     * @property to The recipient's phone number (raw string, validated by the factory).
     */
    data class Sms(
        override val id: String,
        override val content: String,
        override val payload: Map<String, Any> = emptyMap(),
        val from: UInt,
        val to: String,
    ) : CreateNotificationInput

    /**
     * Raw input data for creating a WhatsApp notification.
     *
     * @property content The message body content (validated for non-blank).
     * @property payload Additional metadata for template parameter rendering.
     * @property from The sender's Brazilian phone number (raw string, validated by the factory).
     * @property to The recipient's Brazilian phone number (raw string, validated by the factory).
     * @property templateName The WhatsApp Business API template name (validated for non-blank).
     */
    data class WhatsApp(
        override val id: String,
        override val content: String,
        override val payload: Map<String, Any> = emptyMap(),
        val from: String,
        val to: String,
        val templateName: String,
    ) : CreateNotificationInput
}
