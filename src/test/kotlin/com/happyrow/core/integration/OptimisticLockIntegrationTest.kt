package com.happyrow.core.integration

import com.happyrow.core.domain.resource.common.driven.ResourceRepository
import com.happyrow.core.domain.resource.common.error.OptimisticLockException
import com.happyrow.core.infrastructure.resource.common.driven.ResourceTable
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import org.koin.java.KoinJavaComponent.getKoin
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class OptimisticLockIntegrationTest : IntegrationTestBase() {

  private val eventsPath = "/event/configuration/api/v1/events"
  private val futureDate = Instant.now().plus(7, ChronoUnit.DAYS).toString()

  private fun contributionsPath(eventId: String, resourceId: String) =
    "$eventsPath/$eventId/resources/$resourceId/contributions"

  private fun resourcesPath(eventId: String) = "$eventsPath/$eventId/resources"

  // ─── Test 1 : Flux nominal ───────────────────────────────────────────

  @Test
  fun `should add contribution and increase resource current_quantity`() = integrationTest {
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

    val resourcesResponse = client.get(resourcesPath(eventId)) {
      header("Authorization", auth)
    }
    resourcesResponse.bodyAsText() shouldContain "\"current_quantity\":5"
  }

  // ─── Test 2 : Auto-création du participant via findOrCreate ──────────

  @Test
  fun `should auto-create participant when unknown user contributes`() = integrationTest {
    val client = authenticatedClient()
    val user1Auth = authHeader(generateToken(email = TEST_USER_EMAIL))
    val user2Auth = authHeader(generateToken(userId = "user-2", email = SECOND_USER_EMAIL))

    val (eventId, resourceId) = createEventAndResource(client, user1Auth)

    val response = client.post(contributionsPath(eventId, resourceId)) {
      header("Authorization", user2Auth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("quantity" to 3))
    }

    response.status shouldBe HttpStatusCode.OK
    response.bodyAsText() shouldContain "\"quantity\":3"

    val participantsResponse = client.get("$eventsPath/$eventId/participants") {
      header("Authorization", user1Auth)
    }
    participantsResponse.bodyAsText() shouldContain SECOND_USER_EMAIL
  }

  // ─── Test 3 : Mise à jour contribution existante avec delta correct ──

  @Test
  fun `should update existing contribution and adjust resource quantity by delta`() = integrationTest {
    val client = authenticatedClient()
    val auth = authHeader()

    val (eventId, resourceId) = createEventAndResource(client, auth)

    client.post(contributionsPath(eventId, resourceId)) {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("quantity" to 3))
    }

    client.post(contributionsPath(eventId, resourceId)) {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("quantity" to 7))
    }

    val resourcesResponse = client.get(resourcesPath(eventId)) {
      header("Authorization", auth)
    }
    // delta = 7 - 3 = +4, so total = 3 + 4 = 7
    resourcesResponse.bodyAsText() shouldContain "\"current_quantity\":7"
  }

  // ─── Test 4 : Deux utilisateurs séquentiels accumulent les quantités ─

  @Test
  fun `two users contributing sequentially should accumulate quantities`() = integrationTest {
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

    val resourcesResponse = client.get(resourcesPath(eventId)) {
      header("Authorization", user1Auth)
    }
    resourcesResponse.bodyAsText() shouldContain "\"current_quantity\":7"
  }

  // ─── Test 5 : Verrou optimiste — version périmée au niveau repository ─

  @Test
  fun `should fail with OptimisticLockException when expected version is stale`() = integrationTest {
    val client = authenticatedClient()
    val auth = authHeader()

    val (eventId, resourceId) = createEventAndResource(client, auth)

    // Add a contribution so the version increments
    client.post(contributionsPath(eventId, resourceId)) {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(mapOf("quantity" to 2))
    }

    // Read current version from DB
    val currentVersion = transaction {
      ResourceTable
        .selectAll().where { ResourceTable.id eq UUID.fromString(resourceId) }
        .single()[ResourceTable.version]
    }
    (currentVersion >= 1) shouldBe true

    // Call updateQuantity with a stale version (currentVersion - 1) — simulates a concurrent modification
    val staleVersion = currentVersion - 1
    val resourceRepository = getKoin().get<ResourceRepository>()
    val result = resourceRepository.updateQuantity(
      resourceId = UUID.fromString(resourceId),
      quantityDelta = 1,
      expectedVersion = staleVersion, // stale: real version is currentVersion
    )

    result.isLeft() shouldBe true
    result.mapLeft { error ->
      val rootCause = generateSequence<Throwable>(error) { it.cause }
        .firstOrNull { it is OptimisticLockException }
      (rootCause is OptimisticLockException) shouldBe true
    }
  }

  // ─── Test 6 : Contributions concurrentes — le verrou détecte les conflits ──

  @Test
  fun `concurrent contributions should trigger optimistic lock conflicts`() = integrationTest {
    val client = authenticatedClient()
    val auth = authHeader()

    val (eventId, resourceId) = createEventAndResource(client, auth)

    val userCount = 5
    val quantityPerUser = 2
    val tokens = (1..userCount).map { i ->
      authHeader(generateToken(userId = "user-$i", email = "user$i@happyrow.com"))
    }

    val results = coroutineScope {
      tokens.map { token ->
        async {
          client.post(contributionsPath(eventId, resourceId)) {
            header("Authorization", token)
            contentType(ContentType.Application.Json)
            setBody(mapOf("quantity" to quantityPerUser))
          }
        }
      }.awaitAll()
    }

    val successes = results.count { it.status == HttpStatusCode.OK }
    val conflicts = results.count { it.status == HttpStatusCode.Conflict }

    // At least one should succeed
    (successes >= 1) shouldBe true
    // Optimistic lock should reject at least one concurrent request
    (conflicts >= 1) shouldBe true
    // All responses should be either OK or Conflict (no 500 errors)
    (successes + conflicts) shouldBe userCount

    // Verify resource version in DB matches number of successful resource updates
    val dbVersion = transaction {
      ResourceTable
        .selectAll().where { ResourceTable.id eq UUID.fromString(resourceId) }
        .single()[ResourceTable.version]
    }
    // version started at 0 (or 1 after resource creation) and incremented once per success
    (dbVersion >= successes) shouldBe true
  }

  // ─── Helpers ─────────────────────────────────────────────────────────

  private suspend fun createEventAndResource(client: io.ktor.client.HttpClient, auth: String): Pair<String, String> {
    val eventResponse = client.post(eventsPath) {
      header("Authorization", auth)
      contentType(ContentType.Application.Json)
      setBody(
        mapOf(
          "name" to "Lock Test Event ${System.nanoTime()}",
          "description" to "Optimistic lock test",
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
