package br.com.olympus.hermes.notification.infrastructure.rest.controllers

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.jupiter.api.Test

@QuarkusTest
class NotificationControllerWhatsAppIT {
    @Test
    fun `POST notifications whatsapp with valid request returns 201 with type WHATSAPP`() {
        val requestBody =
            mapOf(
                "content" to "Hello, your order is confirmed!",
                "from" to "11999887766",
                "to" to "11988776655",
                "templateName" to "order_confirmation",
                "payload" to emptyMap<String, Any>(),
            )

        given()
            .log()
            .all()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .`when`()
            .post("/notifications/whatsapp")
            .then()
            .log()
            .all()
            .statusCode(201)
            .body("id", notNullValue())
    }

    @Test
    fun `POST notifications whatsapp response body contains notification id`() {
        val requestBody =
            mapOf(
                "content" to "Hello, your delivery is on the way!",
                "from" to "11999887766",
                "to" to "11988776655",
                "templateName" to "delivery_update",
            )

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .`when`()
            .post("/notifications/whatsapp")
            .then()
            .statusCode(201)
            .body("id", notNullValue())
    }

    @Test
    fun `POST notifications whatsapp with blank content returns 400`() {
        val requestBody =
            mapOf(
                "content" to "",
                "from" to "11999887766",
                "to" to "11988776655",
                "templateName" to "order_confirmation",
            )

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .`when`()
            .post("/notifications/whatsapp")
            .then()
            .statusCode(400)
    }

    @Test
    fun `POST notifications whatsapp with invalid from phone returns 400`() {
        val requestBody =
            mapOf(
                "content" to "Hello!",
                "from" to "invalid-from",
                "to" to "11988776655",
                "templateName" to "order_confirmation",
            )

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .`when`()
            .post("/notifications/whatsapp")
            .then()
            .statusCode(400)
    }

    @Test
    fun `POST notifications whatsapp with invalid to phone returns 400`() {
        val requestBody =
            mapOf(
                "content" to "Hello!",
                "from" to "11999887766",
                "to" to "invalid-to",
                "templateName" to "order_confirmation",
            )

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .`when`()
            .post("/notifications/whatsapp")
            .then()
            .statusCode(400)
    }

    @Test
    fun `POST notifications whatsapp with blank templateName returns 400`() {
        val requestBody =
            mapOf(
                "content" to "Hello!",
                "from" to "11999887766",
                "to" to "11988776655",
                "templateName" to "",
            )

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .`when`()
            .post("/notifications/whatsapp")
            .then()
            .statusCode(400)
    }

    @Test
    fun `POST notifications whatsapp with all invalid fields returns 400 with accumulated errors`() {
        val requestBody =
            mapOf(
                "content" to "",
                "from" to "invalid-from",
                "to" to "invalid-to",
                "templateName" to "",
            )

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .`when`()
            .post("/notifications/whatsapp")
            .then()
            .statusCode(400)
    }
}
