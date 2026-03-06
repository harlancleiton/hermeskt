package br.com.olympus.hermes.infrastructure.rest.controllers

import br.com.olympus.hermes.shared.domain.repositories.NotificationViewRepository
import br.com.olympus.hermes.shared.infrastructure.readmodel.NotificationView
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import jakarta.inject.Inject
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.jupiter.api.Test
import java.util.Date
import java.util.UUID

@QuarkusTest
class NotificationControllerReadIT {
    @Inject lateinit var viewRepository: NotificationViewRepository

    @Test
    fun `GET notification by id should return 200 and the notification`() {
        val id = UUID.randomUUID().toString()
        val view =
            NotificationView().apply {
                this.id = id
                type = "EMAIL"
                status = "PENDING"
                content = "Read integration test"
                to = "test@example.com"
                createdAt = Date()
                updatedAt = Date()
            }
        viewRepository.upsert(view)

        given()
            .contentType(ContentType.JSON)
            .`when`()
            .get("/notifications/$id")
            .then()
            .statusCode(200)
            .body("id", equalTo(id))
            .body("type", equalTo("EMAIL"))
            .body("status", equalTo("PENDING"))
            .body("content", equalTo("Read integration test"))
            .body("to", equalTo("test@example.com"))
            .body("createdAt", notNullValue())
    }

    @Test
    fun `GET notification by id should return 404 when not found`() {
        val id = UUID.randomUUID().toString()

        given()
            .contentType(ContentType.JSON)
            .`when`()
            .get("/notifications/$id")
            .then()
            .statusCode(404)
            .body("message", equalTo("Notification not found"))
    }

    @Test
    fun `GET list notifications should return 200 and paginated result`() {
        val id = UUID.randomUUID().toString()
        val view =
            NotificationView().apply {
                this.id = id
                type = "PUSH"
                status = "PENDING"
                content = "List integration test"
                to = "token123"
                createdAt = Date()
                updatedAt = Date()
            }
        viewRepository.upsert(view)

        given()
            .contentType(ContentType.JSON)
            .queryParam("page", 1)
            .queryParam("size", 10)
            .queryParam("type", "PUSH")
            .queryParam("status", "PENDING")
            .`when`()
            .get("/notifications")
            .then()
            .statusCode(200)
            .body("items", notNullValue())
            .body("totalCount", notNullValue())
            .body("pageIndex", equalTo(1))
            .body("pageSize", equalTo(10))
            .body("totalPages", notNullValue())
    }
}
