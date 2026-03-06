package br.com.olympus.hermes.notification.infrastructure.rest.controllers

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.jupiter.api.Test

@QuarkusTest
class NotificationControllerSmsIT {
    @Test
    fun `POST notifications sms with valid request returns 201 with type SMS`() {
        val requestBody =
            mapOf(
                "content" to "Your OTP is 123456",
                "from" to 12345,
                "to" to "11999887766",
                "payload" to emptyMap<String, Any>(),
            )

        given()
            .log()
            .all()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .`when`()
            .post("/notifications/sms")
            .then()
            .log()
            .all()
            .statusCode(201)
            .body("id", notNullValue())
    }

    @Test
    fun `POST notifications sms response body contains notification id`() {
        val requestBody =
            mapOf(
                "content" to "Your OTP is 654321",
                "from" to 12345,
                "to" to "11999887766",
            )

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .`when`()
            .post("/notifications/sms")
            .then()
            .statusCode(201)
            .body("id", notNullValue())
    }

    @Test
    fun `POST notifications sms with blank content returns 400`() {
        val requestBody =
            mapOf(
                "content" to "",
                "from" to 12345,
                "to" to "11999887766",
            )

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .`when`()
            .post("/notifications/sms")
            .then()
            .statusCode(400)
    }

    @Test
    fun `POST notifications sms with invalid phone number returns 400 with InvalidPhoneError`() {
        val requestBody =
            mapOf(
                "content" to "Your OTP is 123456",
                "from" to 12345,
                "to" to "invalid-phone",
            )

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .`when`()
            .post("/notifications/sms")
            .then()
            .statusCode(400)
    }

    @Test
    fun `POST notifications sms with blank content AND invalid phone returns 400 with accumulated errors`() {
        val requestBody =
            mapOf(
                "content" to "",
                "from" to 12345,
                "to" to "invalid",
            )

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .`when`()
            .post("/notifications/sms")
            .then()
            .statusCode(400)
    }
}
