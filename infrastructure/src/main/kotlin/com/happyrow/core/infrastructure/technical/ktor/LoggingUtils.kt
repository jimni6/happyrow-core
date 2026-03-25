package com.happyrow.core.infrastructure.technical.ktor

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val logger: Logger = LoggerFactory.getLogger(
  "com.happyrow.core.infrastructure.technical.ktor",
)

suspend inline fun ApplicationCall.logAndRespond(problem: ProblemDetail, failure: Exception? = null) {
  if (failure != null) {
    logger.error("Call error: ${problem.detail}", failure)
  } else {
    logger.error("Call error: ${problem.detail}")
  }

  respond(
    status = HttpStatusCode.fromValue(problem.status),
    problem,
  )
}
