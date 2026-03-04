package br.com.olympus.hermes.shared.domain.entities

import br.com.olympus.hermes.shared.domain.events.WhatsAppNotificationCreatedEvent
import br.com.olympus.hermes.shared.domain.valueobjects.BrazilianPhone
import br.com.olympus.hermes.shared.domain.valueobjects.EntityId
import java.util.Date

/**
 * Aggregate root representing a WhatsApp notification. Uses the WhatsApp Business API template
 * model, requiring a [templateName] to identify the message template to send.
 *
 * @property from The sender's Brazilian phone number (WhatsApp Business number).
 * @property to The recipient's Brazilian phone number.
 * @property templateName The WhatsApp Business API template name to use for the message.
 */
class WhatsAppNotification(
    content: String,
    payload: Map<String, Any>,
    shippingReceipt: Any?,
    sentAt: Date?,
    deliveryAt: Date?,
    seenAt: Date?,
    id: EntityId,
    createdAt: Date,
    updatedAt: Date,
    val from: BrazilianPhone,
    val to: BrazilianPhone,
    val templateName: String,
    private val isNew: Boolean,
) : Notification(
        content = content,
        payload = payload,
        shippingReceipt = shippingReceipt,
        sentAt = sentAt,
        deliveryAt = deliveryAt,
        seenAt = seenAt,
        id = id,
        createdAt = createdAt,
        updatedAt = updatedAt,
    ) {
    init {
        if (isNew) {
            applyChange(
                WhatsAppNotificationCreatedEvent(
                    aggregateId = id.value.toString(),
                    content = content,
                    payload = payload,
                    from = from,
                    to = to,
                    templateName = templateName,
                ),
            )
        }
    }
}
