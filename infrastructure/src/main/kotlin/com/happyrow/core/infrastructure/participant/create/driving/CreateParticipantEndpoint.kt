package com.happyrow.core.infrastructure.participant.create.driving

import arrow.core.Either
import arrow.core.flatMap
import com.happyrow.core.domain.event.common.EventAccessControl
import com.happyrow.core.domain.event.common.error.ForbiddenAccessException
import com.happyrow.core.domain.participant.create.CreateParticipantUseCase
import com.happyrow.core.domain.participant.create.error.CreateParticipantException
import com.happyrow.core.infrastructure.common.error.BadRequestException
import com.happyrow.core.infrastructure.participant.common.dto.toDto
import com.happyrow.core.infrastructure.participant.create.driving.dto.CreateParticipantRequestDto
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

private const val FORBIDDEN_ERROR_TYPE = "FORBIDDEN"

fun Route.createParticipantEndpoint(
  createParticipantUseCase: CreateParticipantUseCase,
  eventAccessControl: EventAccessControl,
) {
  post {
    val eventId = call.parameters["eventId"]?.let { UUID.fromString(it) }
      ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing eventId")

    Either.catch {
      val user = call.authenticatedUser()
      Triple(user.userId, user.email, call.receive<CreateParticipantRequestDto>())
    }
      .mapLeft { BadRequestException.InvalidBodyException(it) }
      .flatMap { (userId, email, requestDto) ->
        eventAccessControl.assertUserHasAccess(userId, email, eventId)
          .map { requestDto }
      }
      .map { it.toDomain(eventId) }
      .flatMap { request -> createParticipantUseCase.execute(request) }
      .map { it.toDto() }
      .fold(
        { it.handleFailure(call) },
        { call.respond(HttpStatusCode.Created, it) },
      )
  }
}

private suspend fun Exception.handleFailure(call: ApplicationCall) = when (this) {
  is BadRequestException -> call.logAndRespond(
    problem = ProblemDetail.of(HttpStatusCode.BadRequest, type, message),
    failure = this,
  )

  is ForbiddenAccessException -> call.logAndRespond(
    problem = ProblemDetail.of(
      HttpStatusCode.Forbidden,
      FORBIDDEN_ERROR_TYPE,
      "You do not have access to this event",
    ),
    failure = this,
  )

  is CreateParticipantException -> call.logAndRespond(
    problem = ProblemDetail.technicalError(),
    failure = this,
  )

  else -> call.logAndRespond(
    problem = ProblemDetail.technicalError(),
    failure = this,
  )
}
