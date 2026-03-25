package com.happyrow.core.infrastructure.technical.ktor

import io.ktor.http.HttpStatusCode

data class ProblemDetail(
  val type: String,
  val title: String,
  val status: Int,
  val detail: String,
) {
  companion object {
    fun of(status: HttpStatusCode, type: String, detail: String) = ProblemDetail(
      type = type,
      title = status.description,
      status = status.value,
      detail = detail,
    )

    fun technicalError() = ProblemDetail(
      type = "TECHNICAL_ERROR",
      title = "Internal Server Error",
      status = HttpStatusCode.InternalServerError.value,
      detail = "An unexpected error occurred",
    )
  }
}
