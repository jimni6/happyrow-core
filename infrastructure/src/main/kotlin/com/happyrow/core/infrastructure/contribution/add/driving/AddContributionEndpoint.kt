package com.happyrow.core.infrastructure.contribution.add.driving

import arrow.core.Either
import arrow.core.flatMap
import com.happyrow.core.domain.contribution.add.AddContributionUseCase
import com.happyrow.core.domain.contribution.add.error.AddContributionException
import com.happyrow.core.domain.resource.common.error.OptimisticLockException
import com.happyrow.core.infrastructure.common.error.BadRequestException
import com.happyrow.core.infrastructure.contribution.add.driving.dto.AddContributionRequestDto
import com.happyrow.core.infrastructure.contribution.common.dto.toDto
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

private const val OPTIMISTIC_LOCK_ERROR_TYPE = "OPTIMISTIC_LOCK_FAILURE"

fun Route.addContributionEndpoint(addContributionUseCase: AddContributionUseCase) {
  post {
    val eventId = call.parameters["eventId"]?.let { UUID.fromString(it) }
      ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing eventId")

    val resourceId = call.parameters["resourceId"]?.let { UUID.fromString(it) }
      ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing resourceId")

    Either.catch {
      val user = call.authenticatedUser()
      val requestDto = call.receive<AddContributionRequestDto>()
      requestDto.toDomain(UUID.fromString(user.userId), eventId, resourceId)
    }
      .mapLeft { BadRequestException.InvalidBodyException(it) }
      .flatMap { request -> addContributionUseCase.execute(request) }
      .map { it.toDto() }
      .fold(
        { it.handleFailure(call) },
        { call.respond(HttpStatusCode.OK, it) },
      )
  }
}

private suspend fun Exception.handleFailure(call: ApplicationCall) = when (this) {
  is BadRequestException -> call.logAndRespond(
    problem = ProblemDetail.of(HttpStatusCode.BadRequest, type = type, detail = message),
    failure = this,
  )

  is AddContributionException -> this.handleAddContributionFailure(call)

  else -> call.logAndRespond(
    problem = ProblemDetail.technicalError(),
    failure = this,
  )
}

private suspend fun AddContributionException.handleAddContributionFailure(call: ApplicationCall) {
  val rootCause = generateSequence<Throwable>(this) { it.cause }
    .firstOrNull { it is OptimisticLockException }

  when (rootCause) {
    is OptimisticLockException -> call.logAndRespond(
      problem = ProblemDetail.of(
        HttpStatusCode.Conflict,
        type = OPTIMISTIC_LOCK_ERROR_TYPE,
        detail = "Resource was modified by another user. Please refresh and try again.",
      ),
      failure = this,
    )

    else -> call.logAndRespond(
      problem = ProblemDetail.technicalError(),
      failure = this,
    )
  }
}
