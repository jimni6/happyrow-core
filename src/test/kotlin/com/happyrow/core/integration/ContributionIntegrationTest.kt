package com.happyrow.core.integration

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.delete
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

class ContributionIntegrationTest : IntegrationTestBase() {

  private val eventsPath = "/event/configuration/api/v1/events"
  private val futureDate = Instant.now().plus(7, ChronoUnit.DAYS).toString()

  private fun contributionsPath(eventId: String, resourceId: String) =
    "$eventsPath/$eventId/resources/$resourceId/contributions"

  @Test
  fun `should add a contribution to a resource`() = integrationTest {
    val client = authenticatedClient()
    val auth = authHeader()

    val (eventId, resourceId) = createEventAndResource(client, auth)

    val response = client.post(contributionsPath(eventId, resourceId)) {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("quantity" to 5))
    }

    response.status shouldBe HttpStatusCode.OK
    response.bodyAsText() shouldContain "\"quantity\":5"
  }

  @Test
  fun `should update an existing contribution quantity`() = integrationTest {
    val client = authenticatedClient()
    val auth = authHeader()

    val (eventId, resourceId) = createEventAndResource(client, auth)

    client.post(contributionsPath(eventId, resourceId)) {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("quantity" to 3))
    }

    val response = client.post(contributionsPath(eventId, resourceId)) {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("quantity" to 7))
    }

    response.status shouldBe HttpStatusCode.OK
    response.bodyAsText() shouldContain "\"quantity\":7"
  }

  @Test
  fun `should reduce a contribution`() = integrationTest {
    val client = authenticatedClient()
    val auth = authHeader()

    val (eventId, resourceId) = createEventAndResource(client, auth)

    client.post(contributionsPath(eventId, resourceId)) {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("quantity" to 10))
    }

    val response = client.post("${contributionsPath(eventId, resourceId)}/reduce") {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("quantity" to 3))
    }

    response.status shouldBe HttpStatusCode.OK
    response.bodyAsText() shouldContain "\"quantity\":7"
  }

  @Test
  fun `should return NoContent when reducing contribution to zero`() = integrationTest {
    val client = authenticatedClient()
    val auth = authHeader()

    val (eventId, resourceId) = createEventAndResource(client, auth)

    client.post(contributionsPath(eventId, resourceId)) {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("quantity" to 5))
    }

    val response = client.post("${contributionsPath(eventId, resourceId)}/reduce") {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("quantity" to 5))
    }

    response.status shouldBe HttpStatusCode.NoContent
  }

  @Test
  fun `should return 400 when reducing more than contributed`() = integrationTest {
    val client = authenticatedClient()
    val auth = authHeader()

    val (eventId, resourceId) = createEventAndResource(client, auth)

    client.post(contributionsPath(eventId, resourceId)) {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("quantity" to 3))
    }

    val response = client.post("${contributionsPath(eventId, resourceId)}/reduce") {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("quantity" to 10))
    }

    response.status shouldBe HttpStatusCode.BadRequest
    response.bodyAsText() shouldContain "INSUFFICIENT_CONTRIBUTION"
  }

  @Test
  fun `should delete a contribution`() = integrationTest {
    val client = authenticatedClient()
    val auth = authHeader()

    val (eventId, resourceId) = createEventAndResource(client, auth)

    client.post(contributionsPath(eventId, resourceId)) {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("quantity" to 5))
    }

    val response = client.delete(contributionsPath(eventId, resourceId)) {
      header("Authorization", auth)
    }
    response.status shouldBe HttpStatusCode.NoContent

    val resourcesResponse = client.get("$eventsPath/$eventId/resources") {
      header("Authorization", auth)
    }
    val body = resourcesResponse.bodyAsText()
    body shouldContain "\"current_quantity\":0"
  }

  @Test
  fun `should return 400 for contribution with zero quantity`() = integrationTest {
    val client = authenticatedClient()
    val auth = authHeader()

    val (eventId, resourceId) = createEventAndResource(client, auth)

    val response = client.post(contributionsPath(eventId, resourceId)) {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("quantity" to 0))
    }

    response.status shouldBe HttpStatusCode.BadRequest
  }

  @Test
  fun `contribution from second user should increase resource quantity`() = integrationTest {
    val client = authenticatedClient()
    val user1Auth = authHeader(generateToken(email = TEST_USER_EMAIL))
    val user2Auth = authHeader(generateToken(userId = "user-2", email = SECOND_USER_EMAIL))

    val (eventId, resourceId) = createEventAndResource(client, user1Auth)

    client.post(contributionsPath(eventId, resourceId)) {
      header("Authorization", user1Auth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("quantity" to 3))
    }

    client.post(contributionsPath(eventId, resourceId)) {
      header("Authorization", user2Auth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("quantity" to 4))
    }

    val resourcesResponse = client.get("$eventsPath/$eventId/resources") {
      header("Authorization", user1Auth)
    }

    val body = resourcesResponse.bodyAsText()
    body shouldContain "\"current_quantity\":7"
  }

  private suspend fun createEventAndResource(client: io.ktor.client.HttpClient, auth: String): Pair<String, String> {
    val eventResponse = client.post(eventsPath) {
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
    val eventId = extractId(eventResponse.bodyAsText())

    val resourceResponse = client.post("$eventsPath/$eventId/resources") {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("name" to "Beer", "category" to "DRINK", "quantity" to 1))
    }
    val resourceId = extractId(resourceResponse.bodyAsText())

    return eventId to resourceId
  }

  private fun extractId(json: String): String {
    val regex = """"identifier"\s*:\s*"([^"]+)"""".toRegex()
    return regex.find(json)?.groupValues?.get(1) ?: error("No identifier found in $json")
  }
}
