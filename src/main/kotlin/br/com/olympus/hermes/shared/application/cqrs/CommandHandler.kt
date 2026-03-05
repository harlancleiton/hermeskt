package br.com.olympus.hermes.shared.application.cqrs

import arrow.core.Either
import br.com.olympus.hermes.shared.domain.exceptions.BaseError

/**
 * Base interface for all command handlers in the CQRS pattern. A command handler receives a
 * [Command] and produces no result — it only mutates state. Results must be obtained via a separate
 * [QueryHandler] (CQS principle).
 *
 * @param C The command type this handler processes.
 */
interface CommandHandler<in C : Command> {
    /**
     * Handles the given command.
     *
     * @param command The command to handle.
     * @return Either a [BaseError] on failure or [Unit] on success.
     */
    fun handle(command: C): Either<BaseError, Unit>
}
