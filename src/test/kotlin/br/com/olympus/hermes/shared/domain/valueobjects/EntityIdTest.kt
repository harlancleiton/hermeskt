package br.com.olympus.hermes.shared.domain.valueobjects

import br.com.olympus.hermes.shared.domain.exceptions.EntityIdException
import com.github.javafaker.Faker
import io.quarkus.test.junit.QuarkusTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
import java.util.*

@QuarkusTest
class EntityIdTest {

    private val faker = Faker()

    @Test
    fun `should create EntityId from UUID`() {
        val uuid = UUID.randomUUID()

        val sut = EntityId.from(uuid)

        assertEquals(uuid, sut.value)
    }

    @Test
    fun `should create EntityId from valid UUID string`() {
        val uuid = UUID.randomUUID()
        val uuidString = uuid.toString()

        val sut = EntityId.from(uuidString)

        assertTrue(sut.isSuccess)
    }

    @Test
    fun `should return correct UUID value from valid UUID string`() {
        val expected = UUID.randomUUID()
        val uuidString = expected.toString()

        val observed = EntityId.from(uuidString).getOrThrow().value

        assertEquals(expected, observed)
    }

    @Test
    fun `should throw EntityIdException when UUID string is invalid`() {
        val invalidUuid = faker.lorem().word()

        val sut = assertThrows<EntityIdException.InvalidUUID> {
            EntityId.from(invalidUuid).getOrThrow()
        }

        assertNotNull(sut)
    }

    @Test
    fun `should contain original invalid value in exception message`() {
        val invalidUuid = faker.lorem().word()

        val sut = assertThrows<EntityIdException.InvalidUUID> {
            EntityId.from(invalidUuid).getOrThrow()
        }

        assertEquals(
            invalidUuid,
            sut.message?.substring("Value '".length, "Value '".length + invalidUuid.length)
        )
    }

    @Test
    fun `should return failure result for invalid UUID string`() {
        val invalidUuid = faker.lorem().word()

        val sut = EntityId.from(invalidUuid)

        assertTrue(sut.isFailure)
    }

    @Test
    fun `should generate new EntityId`() {
        val sut = EntityId.generate()

        assertNotNull(sut.value)
    }

    @Test
    fun `should generate EntityId with valid UUID`() {
        val sut = EntityId.generate()

        assertNotNull(UUID.fromString(sut.value.toString()))
    }

    @Test
    fun `should generate different EntityIds on multiple calls`() {
        val first = EntityId.generate()
        val second = EntityId.generate()

        val sut = first.value != second.value

        assertTrue(sut)
    }

    @Test
    fun `should handle empty string as invalid UUID`() {
        val emptyString = ""

        val sut = EntityId.from(emptyString)

        assertTrue(sut.isFailure)
    }

    @Test
    fun `should handle null UUID string as invalid`() {
        val nullString = "null"

        val sut = EntityId.from(nullString)

        assertTrue(sut.isFailure)
    }

    @Test
    fun `should create equal EntityIds from same UUID`() {
        val uuid = UUID.randomUUID()
        val expected = EntityId.from(uuid)

        val observed = EntityId.from(uuid)

        assertEquals(expected, observed)
    }

    @Test
    fun `should create equal EntityIds from same UUID string`() {
        val uuidString = UUID.randomUUID().toString()
        val expected = EntityId.from(uuidString).getOrThrow()

        val observed = EntityId.from(uuidString).getOrThrow()

        assertEquals(expected, observed)
    }

    @Test
    fun `should handle UUID with uppercase letters`() {
        val uuid = UUID.randomUUID()
        val uppercaseUuidString = uuid.toString().uppercase()

        val sut = EntityId.from(uppercaseUuidString)

        assertTrue(sut.isSuccess)
    }

    @Test
    fun `should handle UUID with mixed case letters`() {
        val uuid = UUID.randomUUID()
        val mixedCaseUuidString = uuid.toString().replaceFirstChar { it.uppercase() }

        val sut = EntityId.from(mixedCaseUuidString)

        assertTrue(sut.isSuccess)
    }

    @Test
    fun `should throw exception with cause when parsing invalid UUID`() {
        val invalidUuid = faker.lorem().word()

        val sut = assertThrows<EntityIdException.InvalidUUID> {
            EntityId.from(invalidUuid).getOrThrow()
        }

        assertNotNull(sut.cause)
    }
}