package br.com.olympus.hermes.shared.domain.factories

/**
 * Enumeration of supported notification types in the system. Each type corresponds to a different
 * notification channel.
 */
enum class NotificationType {
    /** Email notification channel */
    EMAIL,

    /** SMS notification channel */
    SMS,

    /** Push notification channel (mobile/web) */
    PUSH,

    /** WhatsApp notification channel */
    WHATSAPP
}
