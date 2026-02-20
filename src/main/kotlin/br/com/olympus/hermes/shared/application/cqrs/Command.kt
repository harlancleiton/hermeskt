package br.com.olympus.hermes.shared.application.cqrs

/**
 * Marker interface for all commands in the CQRS pattern. A command represents an intent
 * to perform a write operation that changes the system state. Commands are immutable
 * data carriers and should contain only the data required to execute the operation.
 */
interface Command
