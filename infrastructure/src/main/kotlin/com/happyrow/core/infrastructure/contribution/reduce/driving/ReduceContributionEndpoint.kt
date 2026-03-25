package com.happyrow.core.infrastructure.contribution.reduce.driving

import arrow.core.Either
import arrow.core.flatMap
import com.happyrow.core.domain.contribution.reduce.ReduceContributionUseCase
import com.happyrow.core.domain.contribution.reduce.error.InsufficientContributionException
import com.happyrow.core.domain.contribution.reduce.error.ReduceContributionException
import com.happyrow.core.infrastructure.common.error.BadRequestException
import com.happyrow.core.infrastructure.contribution.common.dto.toDto
import com.happyrow.core.infrastructure.contribution.reduce.driving.dto.ReduceContributionRequestDto
import com.happyrow.core.infrastructure.technical.auth.authenticatedUser
import com.happyrow.core.infrastructure.technical.ktor.ProblemDetail
import com.happyrow.core.infrastructure.technical.ktor.logAndRespond
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import java.util.UUID

private const val INSUFFICIENT_CONTRIBUTION_ERROR_TYPE = "INSUFFICIENT_CONTRIBUTION"

fun Route.reduceContributionEndpoint(reduceContributionUseCase: ReduceContributionUseCase) {
  post("/reduce") {
    val eventId = call.parameters["eventId"]?.let { UUID.fromString(it) }
      ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing eventId")

    val resourceId = call.parameters["resourceId"]?.let { UUID.fromString(it) }
      ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing resourceId")

    Either.catch {
      val user = call.authenticatedUser()
      val requestDto = call.receive<ReduceContributionRequestDto>()
      requestDto.toDomain(UUID.fromString(user.userId), eventId, resourceId)
    }
      .mapLeft { BadRequestException.InvalidBodyException(it) }
      .flatMap { request -> reduceContributionUseCase.execute(request) }
      .map { contribution -> contribution?.toDto() }
      .fold(
        { it.handleFailure(call) },
        { contribution ->
          if (contribution == null) {
            call.respond(HttpStatusCode.NoContent)
          } else {
            call.respond(HttpStatusCode.OK, contribution)
          }
        },
      )
  }
}

private suspend fun Exception.handleFailure(call: ApplicationCall) = when (this) {
  is BadRequestException -> call.logAndRespond(
    problem = ProblemDetail.of(HttpStatusCode.BadRequest, type = type, detail = message),
    failure = this,
  )

  is ReduceContributionException -> this.handleReduceContributionFailure(call)

  else -> call.logAndRespond(
    problem = ProblemDetail.technicalError(),
    failure = this,
  )
}

private suspend fun ReduceContributionException.handleReduceContributionFailure(call: ApplicationCall) {
  val rootCause = generateSequence<Throwable>(this.cause) { it.cause }.lastOrNull()

  when (rootCause) {
    is InsufficientContributionException -> call.logAndRespond(
      problem = ProblemDetail.of(
        HttpStatusCode.BadRequest,
        type = INSUFFICIENT_CONTRIBUTION_ERROR_TYPE,
        detail = rootCause.message ?: "Cannot reduce by requested quantity",
      ),
      failure = this,
    )

    else -> call.logAndRespond(
      problem = ProblemDetail.technicalError(),
      failure = this,
    )
  }
}
