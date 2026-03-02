package com.happyrow.core.integration

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
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

class ParticipantIntegrationTest : IntegrationTestBase() {

  private val eventsPath = "/event/configuration/api/v1/events"
  private val futureDate = Instant.now().plus(7, ChronoUnit.DAYS).toString()

  private fun participantsPath(eventId: String) = "$eventsPath/$eventId/participants"

  @Test
  fun `creating an event should auto-create a confirmed participant for the creator`() = integrationTest {
    val client = authenticatedClient()
    val auth = authHeader()

    val createResponse = client.post(eventsPath) {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(
        mapOf(
          "name" to "Team Dinner",
          "description" to "desc",
          "event_date" to futureDate,
          "location" to "Paris",
          "type" to "DINER",
        ),
      )
    }
    val eventId = extractId(createResponse.bodyAsText())

    val response = client.get(participantsPath(eventId)) {
      header("Authorization", auth)
    }
    response.status shouldBe HttpStatusCode.OK
    val body = response.bodyAsText()
    body shouldContain TEST_USER_EMAIL
    body shouldContain "CONFIRMED"
  }

  @Test
  fun `should create a new participant`() = integrationTest {
    val client = authenticatedClient()
    val auth = authHeader()

    val eventId = createEvent(client, auth)

    val response = client.post(participantsPath(eventId)) {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(
        mapOf(
          "user_email" to "friend@example.com",
          "status" to "INVITED",
        ),
      )
    }
    response.status shouldBe HttpStatusCode.Created
    val body = response.bodyAsText()
    body shouldContain "friend@example.com"
    body shouldContain "INVITED"
  }

  @Test
  fun `should get all participants for an event`() = integrationTest {
    val client = authenticatedClient()
    val auth = authHeader()

    val eventId = createEvent(client, auth)

    client.post(participantsPath(eventId)) {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("user_email" to "alice@example.com", "status" to "CONFIRMED"))
    }
    client.post(participantsPath(eventId)) {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("user_email" to "bob@example.com", "status" to "MAYBE"))
    }

    val response = client.get(participantsPath(eventId)) {
      header("Authorization", auth)
    }
    response.status shouldBe HttpStatusCode.OK
    val body = response.bodyAsText()
    body shouldContain TEST_USER_EMAIL
    body shouldContain "alice@example.com"
    body shouldContain "bob@example.com"
  }

  @Test
  fun `should update a participant status`() = integrationTest {
    val client = authenticatedClient()
    val auth = authHeader()

    val eventId = createEvent(client, auth)

    client.post(participantsPath(eventId)) {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("user_email" to "guest@example.com", "status" to "INVITED"))
    }

    val response = client.put("${participantsPath(eventId)}/guest@example.com") {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("status" to "CONFIRMED"))
    }
    response.status shouldBe HttpStatusCode.OK
    response.bodyAsText() shouldContain "CONFIRMED"
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
