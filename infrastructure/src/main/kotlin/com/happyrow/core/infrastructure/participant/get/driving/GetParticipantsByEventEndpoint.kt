package com.happyrow.core.infrastructure.participant.get.driving

import arrow.core.Either
import arrow.core.flatMap
import com.happyrow.core.domain.event.common.EventAccessControl
import com.happyrow.core.domain.event.common.error.ForbiddenAccessException
import com.happyrow.core.domain.participant.get.GetParticipantsByEventUseCase
import com.happyrow.core.domain.participant.get.error.GetParticipantsException
import com.happyrow.core.infrastructure.common.error.BadRequestException
import com.happyrow.core.infrastructure.participant.common.dto.toDto
import com.happyrow.core.infrastructure.technical.auth.authenticatedUser
import com.happyrow.core.infrastructure.technical.ktor.ClientErrorMessage
import com.happyrow.core.infrastructure.technical.ktor.ClientErrorMessage.Companion.technicalErrorMessage
import com.happyrow.core.infrastructure.technical.ktor.logAndRespond
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import java.util.UUID

private const val FORBIDDEN_ERROR_TYPE = "FORBIDDEN"

fun Route.getParticipantsByEventEndpoint(
  getParticipantsByEventUseCase: GetParticipantsByEventUseCase,
  eventAccessControl: EventAccessControl,
) {
  get {
    Either.catch {
      val user = call.authenticatedUser()
      val eventId = call.parameters["eventId"]?.let { UUID.fromString(it) }
        ?: throw IllegalArgumentException("Missing eventId")
      Triple(user.userId, user.email, eventId)
    }
      .mapLeft {
        BadRequestException.InvalidParameterException(
          "auth_or_eventId",
          it.message ?: "Authentication or eventId error",
        )
      }
      .flatMap { (userId, email, eventId) ->
        eventAccessControl.assertUserHasAccess(userId, email, eventId)
          .map { eventId }
      }
      .flatMap { eventId -> getParticipantsByEventUseCase.execute(eventId) }
      .map { participants -> participants.map { it.toDto() } }
      .fold(
        { it.handleFailure(call) },
        { call.respond(HttpStatusCode.OK, it) },
      )
  }
}

private suspend fun Exception.handleFailure(call: ApplicationCall) = when (this) {
  is ForbiddenAccessException -> call.logAndRespond(
    status = HttpStatusCode.Forbidden,
    responseMessage = ClientErrorMessage.of(
      type = FORBIDDEN_ERROR_TYPE,
      detail = "You do not have access to this event",
    ),
    failure = this,
  )

  is GetParticipantsException -> call.logAndRespond(
    status = HttpStatusCode.InternalServerError,
    responseMessage = technicalErrorMessage(),
    failure = this,
  )

  else -> call.logAndRespond(
    status = HttpStatusCode.InternalServerError,
    responseMessage = technicalErrorMessage(),
    failure = this,
  )
}
