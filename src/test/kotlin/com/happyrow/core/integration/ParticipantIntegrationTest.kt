package com.happyrow.core.integration

import com.happyrow.core.integration.IntegrationTestBase.Companion.TEST_USER_ID
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
import java.util.UUID

class ParticipantIntegrationTest : IntegrationTestBase() {

  private val eventsPath = "/event/configuration/api/v1/events"
  private val futureDate = Instant.now().plus(7, ChronoUnit.DAYS).toString()

  private val friendUserId = UUID.fromString("c1111111-1111-4111-8111-111111111111")
  private val aliceUserId = UUID.fromString("a1111111-1111-4111-8111-111111111111")
  private val bobUserId = UUID.fromString("b1111111-1111-4111-8111-111111111111")
  private val guestUserId = UUID.fromString("g1111111-1111-4111-8111-111111111111")

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
    body shouldContain TEST_USER_ID
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
          "user_id" to friendUserId.toString(),
          "status" to "INVITED",
        ),
      )
    }
    response.status shouldBe HttpStatusCode.Created
    val body = response.bodyAsText()
    body shouldContain friendUserId.toString()
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
      setBody(mapOf("user_id" to aliceUserId.toString(), "status" to "CONFIRMED"))
    }
    client.post(participantsPath(eventId)) {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("user_id" to bobUserId.toString(), "status" to "MAYBE"))
    }

    val response = client.get(participantsPath(eventId)) {
      header("Authorization", auth)
    }
    response.status shouldBe HttpStatusCode.OK
    val body = response.bodyAsText()
    body shouldContain TEST_USER_ID
    body shouldContain aliceUserId.toString()
    body shouldContain bobUserId.toString()
  }

  @Test
  fun `should update a participant status`() = integrationTest {
    val client = authenticatedClient()
    val auth = authHeader()

    val eventId = createEvent(client, auth)

    client.post(participantsPath(eventId)) {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("user_id" to guestUserId.toString(), "status" to "INVITED"))
    }

    val response = client.put("${participantsPath(eventId)}/$guestUserId") {
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
