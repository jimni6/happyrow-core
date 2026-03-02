package com.happyrow.core.integration

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Test

class AuthIntegrationTest : IntegrationTestBase() {

  @Test
  fun `public route root should be accessible without token`() = integrationTest {
    val client = authenticatedClient()
    val response = client.get("/")
    response.status shouldBe HttpStatusCode.OK
    response.bodyAsText() shouldContain "happyrow-core"
  }

  @Test
  fun `public route info should be accessible without token`() = integrationTest {
    val client = authenticatedClient()
    val response = client.get("/info")
    response.status shouldBe HttpStatusCode.OK
    response.bodyAsText() shouldContain "happyrow-core"
  }

  @Test
  fun `protected route should return 401 without token`() = integrationTest {
    val client = authenticatedClient()
    val response = client.get("/event/configuration/api/v1/events")
    response.status shouldBe HttpStatusCode.Unauthorized
    response.bodyAsText() shouldContain "MISSING_TOKEN"
  }

  @Test
  fun `protected route should return 401 with invalid token`() = integrationTest {
    val client = authenticatedClient()
    val response = client.get("/event/configuration/api/v1/events") {
      header("Authorization", "Bearer invalid-token")
    }
    response.status shouldBe HttpStatusCode.Unauthorized
    response.bodyAsText() shouldContain "INVALID_TOKEN"
  }

  @Test
  fun `protected route should accept valid token`() = integrationTest {
    val client = authenticatedClient()
    val response = client.get("/event/configuration/api/v1/events") {
      header("Authorization", authHeader())
    }
    response.status shouldBe HttpStatusCode.OK
  }
}
