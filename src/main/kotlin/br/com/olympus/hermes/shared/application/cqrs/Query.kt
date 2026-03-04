package br.com.olympus.hermes.shared.application.cqrs

/**
 * Marker interface for all queries in the CQRS pattern. A query represents a read operation
 * that retrieves data without modifying system state. Queries always read from the read model
 * (MongoDB) and never from the write store (DynamoDB).
 *
 * @param R The type of result this query returns.
 */
interface Query<R>
