package br.com.olympus.hermes.infrastructure.rest.controllers

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.jupiter.api.Test

@QuarkusTest
class NotificationControllerPushIT {
    @Test
    fun `POST notifications push with valid request returns 201`() {
        val requestBody =
            mapOf(
                "deviceToken" to "validToken",
                "title" to "Push Title",
                "body" to "Push Body",
                "payload" to emptyMap<String, Any>(),
                "data" to emptyMap<String, String>(),
            )

        given()
            .log()
            .all()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .`when`()
            .post("/notifications/push")
            .then()
            .log()
            .all()
            .statusCode(201)
            .body("id", notNullValue())
    }

    @Test
    fun `POST notifications push with blank deviceToken returns 400`() {
        val requestBody =
            mapOf(
                "deviceToken" to "",
                "title" to "Push Title",
                "body" to "Push Body",
            )

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .`when`()
            .post("/notifications/push")
            .then()
            .statusCode(400)
    }

    @Test
    fun `POST notifications push with blank title returns 400`() {
        val requestBody =
            mapOf(
                "deviceToken" to "token",
                "title" to "",
                "body" to "Push Body",
            )

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .`when`()
            .post("/notifications/push")
            .then()
            .statusCode(400)
    }

    @Test
    fun `POST notifications push with blank body returns 400`() {
        val requestBody =
            mapOf(
                "deviceToken" to "token",
                "title" to "title",
                "body" to "",
            )

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .`when`()
            .post("/notifications/push")
            .then()
            .statusCode(400)
    }

    @Test
    fun `POST notifications push with multiple invalid fields returns 400 with accumulated errors`() {
        val requestBody =
            mapOf(
                "deviceToken" to "",
                "title" to "",
                "body" to "",
            )

        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .`when`()
            .post("/notifications/push")
            .then()
            .statusCode(400)
    }
}
