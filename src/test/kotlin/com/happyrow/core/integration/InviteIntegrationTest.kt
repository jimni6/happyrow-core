package com.happyrow.core.integration

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
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

class InviteIntegrationTest : IntegrationTestBase() {

  private val eventsPath = "/event/configuration/api/v1/events"
  private val invitesPath = "/event/configuration/api/v1/invites"
  private val futureDate = Instant.now().plus(7, ChronoUnit.DAYS).toString()

  private suspend fun createEvent(
    client: io.ktor.client.HttpClient,
    auth: String,
    name: String = "Test Event",
  ): String {
    val response = client.post(eventsPath) {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(
        mapOf(
          "name" to name,
          "description" to "A test event",
          "event_date" to futureDate,
          "location" to "Paris",
          "type" to "BIRTHDAY",
        ),
      )
    }
    response.status shouldBe HttpStatusCode.Created
    return extractField(response.bodyAsText(), "identifier")
  }

  private fun extractField(json: String, field: String): String {
    val regex = """"$field"\s*:\s*"([^"]+)"""".toRegex()
    return regex.find(json)?.groupValues?.get(1) ?: error("No $field found in $json")
  }

  private fun extractToken(json: String): String = extractField(json, "token")

  // --- BACK-001: Generate invite link ---

  @Test
  fun `should generate invite link for organizer`() = integrationTest {
    val client = authenticatedClient()
    val auth = authHeader()
    val eventId = createEvent(client, auth)

    val response = client.post("$eventsPath/$eventId/invites") {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("expires_in_days" to 7))
    }

    response.status shouldBe HttpStatusCode.Created
    val body = response.bodyAsText()
    body shouldContain "token"
    body shouldContain "invite_url"
    body shouldContain "ACTIVE"
    body shouldContain eventId
  }

  @Test
  fun `should return 403 when non-organizer generates invite`() = integrationTest {
    val client = authenticatedClient()
    val creatorAuth = authHeader()
    val otherAuth = authHeader(generateToken(userId = SECOND_USER_ID, email = SECOND_USER_EMAIL))
    val eventId = createEvent(client, creatorAuth)

    val response = client.post("$eventsPath/$eventId/invites") {
      header("Authorization", otherAuth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("expires_in_days" to 7))
    }

    response.status shouldBe HttpStatusCode.Forbidden
  }

  @Test
  fun `should return 409 when active invite already exists`() = integrationTest {
    val client = authenticatedClient()
    val auth = authHeader()
    val eventId = createEvent(client, auth)

    client.post("$eventsPath/$eventId/invites") {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("expires_in_days" to 7))
    }.status shouldBe HttpStatusCode.Created

    val response = client.post("$eventsPath/$eventId/invites") {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("expires_in_days" to 7))
    }

    response.status shouldBe HttpStatusCode.Conflict
  }

  @Test
  fun `should return 401 when not authenticated`() = integrationTest {
    val client = authenticatedClient()

    val response = client.post("$eventsPath/00000000-0000-0000-0000-000000000000/invites") {
      contentType(ContentType.Application.Json)
      setBody(mapOf("expires_in_days" to 7))
    }

    response.status shouldBe HttpStatusCode.Unauthorized
  }

  // --- BACK-002: Validate invite token ---

  @Test
  fun `should validate token and return event details`() = integrationTest {
    val client = authenticatedClient()
    val auth = authHeader()
    val eventId = createEvent(client, auth, "Birthday Party")

    val createInvite = client.post("$eventsPath/$eventId/invites") {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("expires_in_days" to 7))
    }
    val token = extractToken(createInvite.bodyAsText())

    val response = client.get("$invitesPath/$token")

    response.status shouldBe HttpStatusCode.OK
    val body = response.bodyAsText()
    body shouldContain "VALID"
    body shouldContain "Birthday Party"
    body shouldContain "Paris"
    body shouldContain "BIRTHDAY"
  }

  @Test
  fun `should return 404 for unknown token`() = integrationTest {
    val client = authenticatedClient()

    val response = client.get("$invitesPath/nonexistenttoken123456")

    response.status shouldBe HttpStatusCode.NotFound
    response.bodyAsText() shouldContain "INVITE_NOT_FOUND"
  }

  @Test
  fun `validate endpoint should not require authentication`() = integrationTest {
    val client = authenticatedClient()
    val auth = authHeader()
    val eventId = createEvent(client, auth)

    val createInvite = client.post("$eventsPath/$eventId/invites") {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("expires_in_days" to 7))
    }
    val token = extractToken(createInvite.bodyAsText())

    val response = client.get("$invitesPath/$token")

    response.status shouldBe HttpStatusCode.OK
    response.bodyAsText() shouldContain "VALID"
  }

  @Test
  fun `should return REVOKED status for revoked token`() = integrationTest {
    val client = authenticatedClient()
    val auth = authHeader()
    val eventId = createEvent(client, auth)

    val createInvite = client.post("$eventsPath/$eventId/invites") {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("expires_in_days" to 7))
    }
    val token = extractToken(createInvite.bodyAsText())

    client.delete("$eventsPath/$eventId/invites/$token") {
      header("Authorization", auth)
    }.status shouldBe HttpStatusCode.NoContent

    val response = client.get("$invitesPath/$token")

    response.status shouldBe HttpStatusCode.OK
    val body = response.bodyAsText()
    body shouldContain "REVOKED"
    body shouldNotContain "Birthday"
  }

  // --- BACK-003: Accept invite ---

  @Test
  fun `should accept invite and become participant`() = integrationTest {
    val client = authenticatedClient()
    val creatorAuth = authHeader()
    val joinerAuth = authHeader(generateToken(userId = SECOND_USER_ID, email = SECOND_USER_EMAIL))
    val eventId = createEvent(client, creatorAuth)

    val createInvite = client.post("$eventsPath/$eventId/invites") {
      header("Authorization", creatorAuth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("expires_in_days" to 7))
    }
    val token = extractToken(createInvite.bodyAsText())

    val response = client.post("$invitesPath/$token/accept") {
      header("Authorization", joinerAuth)
    }

    response.status shouldBe HttpStatusCode.OK
    val body = response.bodyAsText()
    body shouldContain eventId
    body shouldContain "CONFIRMED"
    body shouldContain SECOND_USER_ID
  }

  @Test
  fun `should return 409 when already participant`() = integrationTest {
    val client = authenticatedClient()
    val creatorAuth = authHeader()
    val joinerAuth = authHeader(generateToken(userId = SECOND_USER_ID, email = SECOND_USER_EMAIL))
    val eventId = createEvent(client, creatorAuth)

    val createInvite = client.post("$eventsPath/$eventId/invites") {
      header("Authorization", creatorAuth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("expires_in_days" to 7))
    }
    val token = extractToken(createInvite.bodyAsText())

    client.post("$invitesPath/$token/accept") {
      header("Authorization", joinerAuth)
    }.status shouldBe HttpStatusCode.OK

    val response = client.post("$invitesPath/$token/accept") {
      header("Authorization", joinerAuth)
    }

    response.status shouldBe HttpStatusCode.Conflict
    response.bodyAsText() shouldContain "ALREADY_PARTICIPANT"
  }

  @Test
  fun `should return 410 when accepting revoked invite`() = integrationTest {
    val client = authenticatedClient()
    val creatorAuth = authHeader()
    val joinerAuth = authHeader(generateToken(userId = SECOND_USER_ID, email = SECOND_USER_EMAIL))
    val eventId = createEvent(client, creatorAuth)

    val createInvite = client.post("$eventsPath/$eventId/invites") {
      header("Authorization", creatorAuth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("expires_in_days" to 7))
    }
    val token = extractToken(createInvite.bodyAsText())

    client.delete("$eventsPath/$eventId/invites/$token") {
      header("Authorization", creatorAuth)
    }.status shouldBe HttpStatusCode.NoContent

    val response = client.post("$invitesPath/$token/accept") {
      header("Authorization", joinerAuth)
    }

    response.status shouldBe HttpStatusCode.Gone
    response.bodyAsText() shouldContain "INVITE_REVOKED"
  }

  @Test
  fun `should return 404 when accepting unknown token`() = integrationTest {
    val client = authenticatedClient()
    val auth = authHeader()

    val response = client.post("$invitesPath/nonexistenttoken123456/accept") {
      header("Authorization", auth)
    }

    response.status shouldBe HttpStatusCode.NotFound
  }

  @Test
  fun `should return 401 when accepting without auth`() = integrationTest {
    val client = authenticatedClient()

    val response = client.post("$invitesPath/sometoken/accept")

    response.status shouldBe HttpStatusCode.Unauthorized
  }

  // --- BACK-004: Get active link & revoke ---

  @Test
  fun `should get active invite link for organizer`() = integrationTest {
    val client = authenticatedClient()
    val auth = authHeader()
    val eventId = createEvent(client, auth)

    client.post("$eventsPath/$eventId/invites") {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("expires_in_days" to 14))
    }.status shouldBe HttpStatusCode.Created

    val response = client.get("$eventsPath/$eventId/invites/active") {
      header("Authorization", auth)
    }

    response.status shouldBe HttpStatusCode.OK
    val body = response.bodyAsText()
    body shouldContain "token"
    body shouldContain "ACTIVE"
    body shouldContain eventId
  }

  @Test
  fun `should return 204 when no active invite`() = integrationTest {
    val client = authenticatedClient()
    val auth = authHeader()
    val eventId = createEvent(client, auth)

    val response = client.get("$eventsPath/$eventId/invites/active") {
      header("Authorization", auth)
    }

    response.status shouldBe HttpStatusCode.NoContent
  }

  @Test
  fun `should return 403 when non-organizer gets active invite`() = integrationTest {
    val client = authenticatedClient()
    val creatorAuth = authHeader()
    val otherAuth = authHeader(generateToken(userId = SECOND_USER_ID, email = SECOND_USER_EMAIL))
    val eventId = createEvent(client, creatorAuth)

    val response = client.get("$eventsPath/$eventId/invites/active") {
      header("Authorization", otherAuth)
    }

    response.status shouldBe HttpStatusCode.Forbidden
  }

  @Test
  fun `should revoke invite link`() = integrationTest {
    val client = authenticatedClient()
    val auth = authHeader()
    val eventId = createEvent(client, auth)

    val createInvite = client.post("$eventsPath/$eventId/invites") {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("expires_in_days" to 7))
    }
    val token = extractToken(createInvite.bodyAsText())

    val response = client.delete("$eventsPath/$eventId/invites/$token") {
      header("Authorization", auth)
    }

    response.status shouldBe HttpStatusCode.NoContent

    val activeResponse = client.get("$eventsPath/$eventId/invites/active") {
      header("Authorization", auth)
    }
    activeResponse.status shouldBe HttpStatusCode.NoContent
  }

  @Test
  fun `should return 409 when revoking already revoked link`() = integrationTest {
    val client = authenticatedClient()
    val auth = authHeader()
    val eventId = createEvent(client, auth)

    val createInvite = client.post("$eventsPath/$eventId/invites") {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("expires_in_days" to 7))
    }
    val token = extractToken(createInvite.bodyAsText())

    client.delete("$eventsPath/$eventId/invites/$token") {
      header("Authorization", auth)
    }.status shouldBe HttpStatusCode.NoContent

    val response = client.delete("$eventsPath/$eventId/invites/$token") {
      header("Authorization", auth)
    }

    response.status shouldBe HttpStatusCode.Conflict
  }

  @Test
  fun `should generate new invite after revoking old one`() = integrationTest {
    val client = authenticatedClient()
    val auth = authHeader()
    val eventId = createEvent(client, auth)

    val firstInvite = client.post("$eventsPath/$eventId/invites") {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("expires_in_days" to 7))
    }
    val firstToken = extractToken(firstInvite.bodyAsText())

    client.delete("$eventsPath/$eventId/invites/$firstToken") {
      header("Authorization", auth)
    }.status shouldBe HttpStatusCode.NoContent

    val secondInvite = client.post("$eventsPath/$eventId/invites") {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("expires_in_days" to 14))
    }

    secondInvite.status shouldBe HttpStatusCode.Created
    val secondToken = extractToken(secondInvite.bodyAsText())
    secondToken shouldNotContain firstToken
  }

  @Test
  fun `should increment current_uses after accept`() = integrationTest {
    val client = authenticatedClient()
    val creatorAuth = authHeader()
    val joinerAuth = authHeader(generateToken(userId = SECOND_USER_ID, email = SECOND_USER_EMAIL))
    val eventId = createEvent(client, creatorAuth)

    val createInvite = client.post("$eventsPath/$eventId/invites") {
      header("Authorization", creatorAuth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("expires_in_days" to 7))
    }
    val token = extractToken(createInvite.bodyAsText())

    client.post("$invitesPath/$token/accept") {
      header("Authorization", joinerAuth)
    }.status shouldBe HttpStatusCode.OK

    val activeResponse = client.get("$eventsPath/$eventId/invites/active") {
      header("Authorization", creatorAuth)
    }
    activeResponse.status shouldBe HttpStatusCode.OK
    activeResponse.bodyAsText() shouldContain """"current_uses":1"""
  }
}
