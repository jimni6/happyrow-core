package com.happyrow.core.integration

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class SecurityIntegrationTest : IntegrationTestBase() {

  private val eventsPath = "/event/configuration/api/v1/events"
  private val futureDate = Instant.now().plus(7, ChronoUnit.DAYS).toString()

  private fun contributionsPath(eventId: String, resourceId: String) =
    "$eventsPath/$eventId/resources/$resourceId/contributions"

  // ─── S2 : JWT expiré ──────────────────────────────────────────────────

  @Test
  fun `S2 — should return 401 with expired JWT`() = integrationTest {
    val client = authenticatedClient()
    val expiredToken = authHeader(generateExpiredToken())

    val (eventId, resourceId) = createEventAndResource(client, authHeader())

    val response = client.post(contributionsPath(eventId, resourceId)) {
      header("Authorization", expiredToken)
      contentType(ContentType.Application.Json)
      setBody(mapOf("quantity" to 3))
    }

    response.status shouldBe HttpStatusCode.Unauthorized
    response.bodyAsText() shouldContain "INVALID_TOKEN"
  }

  // ─── S4 : Quantité négative ───────────────────────────────────────────

  @Test
  fun `S4 — should return 400 for negative quantity`() = integrationTest {
    val client = authenticatedClient()
    val auth = authHeader()

    val (eventId, resourceId) = createEventAndResource(client, auth)

    val response = client.post(contributionsPath(eventId, resourceId)) {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("quantity" to -5))
    }

    response.status shouldBe HttpStatusCode.BadRequest
  }

  // ─── S5 : Quantité non-numérique ─────────────────────────────────────

  @Test
  fun `S5 — should return 400 for non-numeric quantity`() = integrationTest {
    val client = authenticatedClient()
    val auth = authHeader()
    val raw = rawClient()

    val (eventId, resourceId) = createEventAndResource(client, auth)

    val response = raw.post(contributionsPath(eventId, resourceId)) {
      header("Authorization", auth)
      setBody(TextContent("""{"quantity": "abc"}""", ContentType.Application.Json))
    }

    response.status shouldBe HttpStatusCode.BadRequest
  }

  // ─── S6 : Injection SQL via le body ───────────────────────────────────

  @Test
  fun `S6 — should return 400 for SQL injection attempt in body`() = integrationTest {
    val client = authenticatedClient()
    val auth = authHeader()
    val raw = rawClient()

    val (eventId, resourceId) = createEventAndResource(client, auth)

    val response = raw.post(contributionsPath(eventId, resourceId)) {
      header("Authorization", auth)
      setBody(
        TextContent(
          """{"quantity": "1; DROP TABLE configuration.contribution;"}""",
          ContentType.Application.Json,
        ),
      )
    }

    response.status shouldBe HttpStatusCode.BadRequest
  }

  // ─── S7 : Usurpation d'identité ──────────────────────────────────────

  @Test
  fun `S7 — contribution should use user id from JWT not email claim`() = integrationTest {
    val client = authenticatedClient()
    val userAAuth = authHeader(generateToken(email = TEST_USER_EMAIL))
    val userBAuth = authHeader(generateToken(userId = IntegrationTestBase.SECOND_USER_ID, email = "user-b-fake-email"))

    val (eventId, resourceId) = createEventAndResource(client, userAAuth)

    val response = client.post(contributionsPath(eventId, resourceId)) {
      header("Authorization", userBAuth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("quantity" to 2))
    }

    response.status shouldBe HttpStatusCode.OK

    val participantsResponse = client.get("$eventsPath/$eventId/participants") {
      header("Authorization", userAAuth)
    }
    val participantsBody = participantsResponse.bodyAsText()
    participantsBody shouldContain IntegrationTestBase.SECOND_USER_ID
    participantsBody shouldNotContain "user-b-fake-email"
  }

  // ─── Helpers ──────────────────────────────────────────────────────────

  private suspend fun createEventAndResource(client: io.ktor.client.HttpClient, auth: String): Pair<String, String> {
    val eventResponse = client.post(eventsPath) {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(
        mapOf(
          "name" to "Security Test Event ${System.nanoTime()}",
          "description" to "Security tests",
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
      setBody(mapOf("name" to "Pizza", "category" to "FOOD", "quantity" to 1))
    }
    val resourceId = extractId(resourceResponse.bodyAsText())

    return eventId to resourceId
  }

  private fun extractId(json: String): String {
    val regex = """"identifier"\s*:\s*"([^"]+)"""".toRegex()
    return regex.find(json)?.groupValues?.get(1) ?: error("No identifier found in $json")
  }
}
