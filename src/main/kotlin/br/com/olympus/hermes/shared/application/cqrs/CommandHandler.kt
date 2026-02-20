package br.com.olympus.hermes.shared.application.cqrs

import arrow.core.Either
import br.com.olympus.hermes.shared.domain.exceptions.BaseError

/**
 * Base interface for all command handlers in the CQRS pattern. A command handler receives
 * a [Command] and produces a result wrapped in [Either] for functional error handling.
 *
 * @param C The command type this handler processes.
 * @param R The result type produced on success.
 */
interface CommandHandler<in C : Command, out R> {
    /**
     * Handles the given command and returns the result.
     *
     * @param command The command to handle.
     * @return Either a [BaseError] on failure or the result [R] on success.
     */
    fun handle(command: C): Either<BaseError, R>
}
