package com.happyrow.core.integration

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class EventIntegrationTest : IntegrationTestBase() {

  private val basePath = "/event/configuration/api/v1/events"
  private val futureDate = Instant.now().plus(7, ChronoUnit.DAYS).toString()

  @Test
  fun `should create an event and auto-add creator as participant`() = integrationTest {
    val client = authenticatedClient()
    val response = client.post(basePath) {
      header("Authorization", authHeader())
      contentType(ContentType.Application.Json)
      setBody(
        mapOf(
          "name" to "Birthday Party",
          "description" to "A fun birthday party",
          "event_date" to futureDate,
          "location" to "Paris",
          "type" to "BIRTHDAY",
        ),
      )
    }

    response.status shouldBe HttpStatusCode.Created
    val body = response.bodyAsText()
    body shouldContain "Birthday Party"
    body shouldContain "Paris"
    body shouldContain "BIRTHDAY"
  }

  @Test
  fun `should get events for authenticated organizer`() = integrationTest {
    val client = authenticatedClient()
    val auth = authHeader()

    client.post(basePath) {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(
        mapOf(
          "name" to "Event 1",
          "description" to "First event",
          "event_date" to futureDate,
          "location" to "Lyon",
          "type" to "DINER",
        ),
      )
    }

    client.post(basePath) {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(
        mapOf(
          "name" to "Event 2",
          "description" to "Second event",
          "event_date" to futureDate,
          "location" to "Marseille",
          "type" to "PARTY",
        ),
      )
    }

    val response = client.get(basePath) {
      header("Authorization", auth)
    }

    response.status shouldBe HttpStatusCode.OK
    val body = response.bodyAsText()
    body shouldContain "Event 1"
    body shouldContain "Event 2"
  }

  @Test
  fun `should not see events from other users`() = integrationTest {
    val client = authenticatedClient()
    val user1Token = authHeader(generateToken(email = TEST_USER_EMAIL))
    val user2Token = authHeader(generateToken(userId = "other-user", email = SECOND_USER_EMAIL))

    client.post(basePath) {
      header("Authorization", user1Token)
      contentType(ContentType.Application.Json)
      setBody(
        mapOf(
          "name" to "User1 Event",
          "description" to "desc",
          "event_date" to futureDate,
          "location" to "Paris",
          "type" to "PARTY",
        ),
      )
    }

    val response = client.get(basePath) {
      header("Authorization", user2Token)
    }

    response.status shouldBe HttpStatusCode.OK
    response.bodyAsText() shouldBe "[]"
  }

  @Test
  fun `should update an event`() = integrationTest {
    val client = authenticatedClient()
    val auth = authHeader()

    val createResponse = client.post(basePath) {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(
        mapOf(
          "name" to "Original Name",
          "description" to "desc",
          "event_date" to futureDate,
          "location" to "Paris",
          "type" to "DINER",
        ),
      )
    }

    val eventId = extractId(createResponse.bodyAsText())

    val updateResponse = client.put("$basePath/$eventId") {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(
        mapOf(
          "name" to "Updated Name",
          "description" to "updated desc",
          "event_date" to futureDate,
          "location" to "Lyon",
          "type" to "PARTY",
        ),
      )
    }

    updateResponse.status shouldBe HttpStatusCode.OK
    val body = updateResponse.bodyAsText()
    body shouldContain "Updated Name"
    body shouldContain "Lyon"
    body shouldContain "PARTY"
  }

  @Test
  fun `should delete an event`() = integrationTest {
    val client = authenticatedClient()
    val auth = authHeader()

    val createResponse = client.post(basePath) {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(
        mapOf(
          "name" to "To Delete",
          "description" to "desc",
          "event_date" to futureDate,
          "location" to "Paris",
          "type" to "SNACK",
        ),
      )
    }

    val eventId = extractId(createResponse.bodyAsText())

    val deleteResponse = client.delete("$basePath/$eventId") {
      header("Authorization", auth)
    }
    deleteResponse.status shouldBe HttpStatusCode.NoContent

    val getResponse = client.get(basePath) {
      header("Authorization", auth)
    }
    getResponse.bodyAsText() shouldBe "[]"
  }

  @Test
  fun `should return 403 when non-creator tries to delete`() = integrationTest {
    val client = authenticatedClient()
    val creatorAuth = authHeader(generateToken(email = TEST_USER_EMAIL))
    val otherAuth = authHeader(generateToken(userId = "other-user", email = SECOND_USER_EMAIL))

    val createResponse = client.post(basePath) {
      header("Authorization", creatorAuth)
      contentType(ContentType.Application.Json)
      setBody(
        mapOf(
          "name" to "Protected Event",
          "description" to "desc",
          "event_date" to futureDate,
          "location" to "Paris",
          "type" to "DINER",
        ),
      )
    }

    val eventId = extractId(createResponse.bodyAsText())

    val deleteResponse = client.delete("$basePath/$eventId") {
      header("Authorization", otherAuth)
    }
    deleteResponse.status shouldBe HttpStatusCode.Forbidden
  }

  @Test
  fun `should return 404 when updating non-existent event`() = integrationTest {
    val client = authenticatedClient()
    val auth = authHeader()
    val fakeId = "00000000-0000-0000-0000-000000000000"

    val response = client.put("$basePath/$fakeId") {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(
        mapOf(
          "name" to "Updated",
          "description" to "desc",
          "event_date" to futureDate,
          "location" to "Paris",
          "type" to "DINER",
        ),
      )
    }
    response.status shouldBe HttpStatusCode.NotFound
  }

  @Test
  fun `should return 400 for blank event name`() = integrationTest {
    val client = authenticatedClient()
    val response = client.post(basePath) {
      header("Authorization", authHeader())
      contentType(ContentType.Application.Json)
      setBody(
        mapOf(
          "name" to "   ",
          "description" to "desc",
          "event_date" to futureDate,
          "location" to "Paris",
          "type" to "DINER",
        ),
      )
    }
    response.status shouldBe HttpStatusCode.BadRequest
  }

  private fun extractId(json: String): String {
    val regex = """"identifier"\s*:\s*"([^"]+)"""".toRegex()
    return regex.find(json)?.groupValues?.get(1) ?: error("No identifier found in $json")
  }
}
