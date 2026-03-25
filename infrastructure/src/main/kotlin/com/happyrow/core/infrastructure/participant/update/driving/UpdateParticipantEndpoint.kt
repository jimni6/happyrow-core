package com.happyrow.core.infrastructure.participant.update.driving

import arrow.core.Either
import arrow.core.flatMap
import com.happyrow.core.domain.participant.update.UpdateParticipantUseCase
import com.happyrow.core.domain.participant.update.error.ForbiddenParticipantUpdateException
import com.happyrow.core.domain.participant.update.error.ParticipantNotFoundException
import com.happyrow.core.domain.participant.update.error.UpdateParticipantException
import com.happyrow.core.infrastructure.common.error.BadRequestException
import com.happyrow.core.infrastructure.participant.common.dto.toDto
import com.happyrow.core.infrastructure.participant.update.driving.dto.UpdateParticipantRequestDto
import com.happyrow.core.infrastructure.technical.auth.authenticatedUser
import com.happyrow.core.infrastructure.technical.ktor.ProblemDetail
import com.happyrow.core.infrastructure.technical.ktor.logAndRespond
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.put
import java.util.UUID

private const val FORBIDDEN_ERROR_TYPE = "FORBIDDEN"
private const val NOT_FOUND_ERROR_TYPE = "NOT_FOUND"

fun Route.updateParticipantEndpoint(updateParticipantUseCase: UpdateParticipantUseCase) {
  put {
    val eventId = call.parameters["eventId"]?.let { UUID.fromString(it) }
      ?: return@put call.respond(HttpStatusCode.BadRequest, "Missing eventId")

    val userEmail = call.parameters["userEmail"]
      ?: return@put call.respond(HttpStatusCode.BadRequest, "Missing userEmail")

    Either.catch {
      val user = call.authenticatedUser()
      val body = call.receive<UpdateParticipantRequestDto>()
      Triple(user.userId, user.email, body)
    }
      .mapLeft { BadRequestException.InvalidBodyException(it) }
      .flatMap { (authenticatedUserId, authenticatedEmail, body) ->
        val request = body.toDomain(userEmail, eventId)
        updateParticipantUseCase.execute(request, authenticatedUserId, authenticatedEmail)
      }
      .map { it.toDto() }
      .fold(
        { it.handleFailure(call) },
        { call.respond(HttpStatusCode.OK, it) },
      )
  }
}

private suspend fun Exception.handleFailure(call: ApplicationCall) = when (this) {
  is BadRequestException -> call.logAndRespond(
    problem = ProblemDetail.of(HttpStatusCode.BadRequest, type, message),
    failure = this,
  )

  is ForbiddenParticipantUpdateException -> call.logAndRespond(
    problem = ProblemDetail.of(
      HttpStatusCode.Forbidden,
      FORBIDDEN_ERROR_TYPE,
      "Not authorized to update this participant's status",
    ),
    failure = this,
  )

  is ParticipantNotFoundException -> call.logAndRespond(
    problem = ProblemDetail.of(
      HttpStatusCode.NotFound,
      NOT_FOUND_ERROR_TYPE,
      "Participant ${this.userEmail} not found for event ${this.eventId}",
    ),
    failure = this,
  )

  is UpdateParticipantException -> call.logAndRespond(
    problem = ProblemDetail.technicalError(),
    failure = this,
  )

  else -> call.logAndRespond(
    problem = ProblemDetail.technicalError(),
    failure = this,
  )
}
