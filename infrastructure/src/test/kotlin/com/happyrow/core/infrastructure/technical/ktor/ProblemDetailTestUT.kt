package com.happyrow.core.infrastructure.technical.ktor

import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Test

class ProblemDetailTestUT {

  @Test
  fun `of should create ProblemDetail with status description as title`() {
    val result = ProblemDetail.of(HttpStatusCode.NotFound, "EVENT_NOT_FOUND", "Event abc not found")

    result.type shouldBe "EVENT_NOT_FOUND"
    result.title shouldBe "Not Found"
    result.status shouldBe 404
    result.detail shouldBe "Event abc not found"
  }

  @Test
  fun `technicalError should create standard 500 response`() {
    val result = ProblemDetail.technicalError()

    result.type shouldBe "TECHNICAL_ERROR"
    result.title shouldBe "Internal Server Error"
    result.status shouldBe 500
    result.detail shouldBe "An unexpected error occurred"
  }

  @Test
  fun `of should support all common HTTP status codes`() {
    val badRequest = ProblemDetail.of(HttpStatusCode.BadRequest, "BAD_REQUEST", "Invalid input")
    badRequest.status shouldBe 400
    badRequest.title shouldBe "Bad Request"

    val forbidden = ProblemDetail.of(HttpStatusCode.Forbidden, "FORBIDDEN", "No access")
    forbidden.status shouldBe 403
    forbidden.title shouldBe "Forbidden"

    val conflict = ProblemDetail.of(HttpStatusCode.Conflict, "CONFLICT", "Duplicate")
    conflict.status shouldBe 409
    conflict.title shouldBe "Conflict"
  }
}
