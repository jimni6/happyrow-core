package com.happyrow.core.infrastructure.event.get.driving

import arrow.core.Either
import arrow.core.flatMap
import com.happyrow.core.domain.event.creator.model.Creator
import com.happyrow.core.domain.event.get.GetEventsByOrganizerUseCase
import com.happyrow.core.domain.event.get.error.GetEventException
import com.happyrow.core.infrastructure.event.common.dto.toDto
import com.happyrow.core.infrastructure.technical.auth.authenticatedUser
import com.happyrow.core.infrastructure.technical.ktor.ClientErrorMessage
import com.happyrow.core.infrastructure.technical.ktor.ClientErrorMessage.Companion.technicalErrorMessage
import com.happyrow.core.infrastructure.technical.ktor.logAndRespond
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

private const val ORGANIZER_ID_PARAM = "organizerId"
private const val MISSING_ORGANIZER_ID_ERROR_TYPE = "MISSING_ORGANIZER_ID"
private const val INVALID_ORGANIZER_ID_ERROR_TYPE = "INVALID_ORGANIZER_ID"

fun Route.getEventsEndpoint(getEventsByOrganizerUseCase: GetEventsByOrganizerUseCase) {
  get {
    Either.catch {
      val user = call.authenticatedUser()
      Creator(user.email)
    }
      .mapLeft { InvalidOrganizerIdException("authenticated_user", it) }
      .flatMap { organizer -> getEventsByOrganizerUseCase.execute(organizer) }
      .map { events -> events.map { it.toDto() } }
      .fold(
        { it.handleFailure(call) },
        { call.respond(HttpStatusCode.OK, it) },
      )
  }
}

private suspend fun Exception.handleFailure(call: ApplicationCall) = when (this) {
  is MissingOrganizerIdException -> call.logAndRespond(
    status = HttpStatusCode.BadRequest,
    responseMessage = ClientErrorMessage.of(
      type = MISSING_ORGANIZER_ID_ERROR_TYPE,
      detail = "Query parameter 'organizerId' is required",
    ),
    failure = this,
  )

  is InvalidOrganizerIdException -> call.logAndRespond(
    status = HttpStatusCode.BadRequest,
    responseMessage = ClientErrorMessage.of(
      type = INVALID_ORGANIZER_ID_ERROR_TYPE,
      detail = "Invalid organizerId: $organizerId",
    ),
    failure = this,
  )

  is GetEventException -> call.logAndRespond(
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

private class MissingOrganizerIdException(cause: Throwable) : Exception(cause)
private class InvalidOrganizerIdException(val organizerId: String, cause: Throwable) : Exception(cause)
