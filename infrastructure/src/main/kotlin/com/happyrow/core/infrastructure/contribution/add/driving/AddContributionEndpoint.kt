package com.happyrow.core.infrastructure.contribution.add.driving

import arrow.core.Either
import arrow.core.flatMap
import com.happyrow.core.domain.contribution.add.AddContributionUseCase
import com.happyrow.core.domain.contribution.add.error.AddContributionException
import com.happyrow.core.domain.resource.common.error.OptimisticLockException
import com.happyrow.core.infrastructure.contribution.add.driving.dto.AddContributionRequestDto
import com.happyrow.core.infrastructure.contribution.common.dto.toDto
import com.happyrow.core.infrastructure.event.common.error.BadRequestException
import com.happyrow.core.infrastructure.technical.auth.authenticatedUser
import com.happyrow.core.infrastructure.technical.ktor.ClientErrorMessage
import com.happyrow.core.infrastructure.technical.ktor.ClientErrorMessage.Companion.technicalErrorMessage
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
      requestDto.toDomain(user.email, eventId, resourceId)
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
    status = HttpStatusCode.BadRequest,
    responseMessage = ClientErrorMessage.of(type = type, detail = message),
    failure = this,
  )

  is AddContributionException -> this.handleAddContributionFailure(call)

  else -> call.logAndRespond(
    status = HttpStatusCode.InternalServerError,
    responseMessage = technicalErrorMessage(),
    failure = this,
  )
}

private suspend fun AddContributionException.handleAddContributionFailure(call: ApplicationCall) {
  val rootCause = generateSequence<Throwable>(this.cause) { it.cause }.lastOrNull()

  when (rootCause) {
    is OptimisticLockException -> call.logAndRespond(
      status = HttpStatusCode.Conflict,
      responseMessage = ClientErrorMessage.of(
        type = OPTIMISTIC_LOCK_ERROR_TYPE,
        detail = "Resource was modified by another user. Please refresh and try again.",
      ),
      failure = this,
    )

    else -> call.logAndRespond(
      status = HttpStatusCode.InternalServerError,
      responseMessage = technicalErrorMessage(),
      failure = this,
    )
  }
}
