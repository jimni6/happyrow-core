package com.happyrow.core.integration

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class ResourceIntegrationTest : IntegrationTestBase() {

  private val eventsPath = "/event/configuration/api/v1/events"
  private val futureDate = Instant.now().plus(7, ChronoUnit.DAYS).toString()

  private fun resourcesPath(eventId: String) = "$eventsPath/$eventId/resources"

  @Test
  fun `should create a resource for an event`() = integrationTest {
    val client = authenticatedClient()
    val auth = authHeader()

    val eventId = createEvent(client, auth)

    val response = client.post(resourcesPath(eventId)) {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(
        mapOf(
          "name" to "Soda",
          "category" to "DRINK",
          "quantity" to 6,
          "suggested_quantity" to 10,
        ),
      )
    }

    response.status shouldBe HttpStatusCode.Created
    val body = response.bodyAsText()
    body shouldContain "Soda"
    body shouldContain "DRINK"
  }

  @Test
  fun `should get resources for an event with contributor info`() = integrationTest {
    val client = authenticatedClient()
    val auth = authHeader()

    val eventId = createEvent(client, auth)

    client.post(resourcesPath(eventId)) {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("name" to "Chips", "category" to "FOOD", "quantity" to 3))
    }

    client.post(resourcesPath(eventId)) {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("name" to "Wine", "category" to "DRINK", "quantity" to 2))
    }

    val response = client.get(resourcesPath(eventId)) {
      header("Authorization", auth)
    }

    response.status shouldBe HttpStatusCode.OK
    val body = response.bodyAsText()
    body shouldContain "Chips"
    body shouldContain "Wine"
    body shouldContain "contributors"
    body shouldContain TEST_USER_EMAIL
  }

  @Test
  fun `should return empty list for event with no resources`() = integrationTest {
    val client = authenticatedClient()
    val auth = authHeader()

    val eventId = createEvent(client, auth)

    val response = client.get(resourcesPath(eventId)) {
      header("Authorization", auth)
    }

    response.status shouldBe HttpStatusCode.OK
    response.bodyAsText() shouldEqualJson "[]"
  }

  @Test
  fun `should return 400 for blank resource name`() = integrationTest {
    val client = authenticatedClient()
    val auth = authHeader()

    val eventId = createEvent(client, auth)

    val response = client.post(resourcesPath(eventId)) {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("name" to "  ", "category" to "FOOD", "quantity" to 1))
    }

    response.status shouldBe HttpStatusCode.BadRequest
  }

  @Test
  fun `should return 400 for zero quantity`() = integrationTest {
    val client = authenticatedClient()
    val auth = authHeader()

    val eventId = createEvent(client, auth)

    val response = client.post(resourcesPath(eventId)) {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("name" to "Water", "category" to "DRINK", "quantity" to 0))
    }

    response.status shouldBe HttpStatusCode.BadRequest
  }

  private suspend fun createEvent(client: io.ktor.client.HttpClient, auth: String): String {
    val response = client.post(eventsPath) {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(
        mapOf(
          "name" to "Test Event ${System.nanoTime()}",
          "description" to "desc",
          "event_date" to futureDate,
          "location" to "Paris",
          "type" to "DINER",
        ),
      )
    }
    return extractId(response.bodyAsText())
  }

  private fun extractId(json: String): String {
    val regex = """"identifier"\s*:\s*"([^"]+)"""".toRegex()
    return regex.find(json)?.groupValues?.get(1) ?: error("No identifier found in $json")
  }
}
