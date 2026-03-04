package br.com.olympus.hermes

import br.com.olympus.hermes.core.application.commands.CreateNotificationCommand
import br.com.olympus.hermes.core.application.commands.CreateNotificationHandler
import io.quarkus.logging.Log
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/hello")
class GreetingResource(
    private val createNotificationHandler: CreateNotificationHandler,
) {
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    fun hello(): String {
        val command =
            CreateNotificationCommand.Email(
                content = "Content",
                payload = mapOf("key1" to "value1"),
                from = "from@domain.com",
                to = "to@domain.com",
                subject = "Subject",
            )

        createNotificationHandler
            .handle(command)
            .onLeft { Log.error("Error creating notification: ${it.message}") }
            .onRight { Log.info("Notification created successfully: ${it.id}") }

        return "Hello, World!"
    }
}
