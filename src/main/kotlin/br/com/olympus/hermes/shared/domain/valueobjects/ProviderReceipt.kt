package br.com.olympus.hermes.shared.domain.valueobjects

/**
 * Represents the receipt returned by an external notification provider after a successful delivery.
 *
 * @property receiptId Provider-specific identifier for the delivery attempt.
 * @property provider Human-readable name of the provider that processed the delivery.
 * @property rawResponse Optional raw response payload from the provider, for diagnostics.
 */
data class ProviderReceipt(
    val receiptId: String,
    val provider: String,
    val rawResponse: Map<String, Any> = emptyMap(),
)
