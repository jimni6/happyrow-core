package com.happyrow.core.infrastructure.participant.delete.driving

import arrow.core.Either
import arrow.core.flatMap
import com.happyrow.core.domain.participant.delete.DeleteParticipantUseCase
import com.happyrow.core.domain.participant.delete.error.DeleteParticipantException
import com.happyrow.core.domain.participant.delete.error.ForbiddenParticipantDeleteException
import com.happyrow.core.domain.participant.update.error.ParticipantNotFoundException
import com.happyrow.core.infrastructure.technical.auth.authenticatedUser
import com.happyrow.core.infrastructure.technical.ktor.ClientErrorMessage
import com.happyrow.core.infrastructure.technical.ktor.ClientErrorMessage.Companion.technicalErrorMessage
import com.happyrow.core.infrastructure.technical.ktor.logAndRespond
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import java.util.UUID

private const val FORBIDDEN_ERROR_TYPE = "FORBIDDEN"
private const val NOT_FOUND_ERROR_TYPE = "NOT_FOUND"

fun Route.deleteParticipantEndpoint(deleteParticipantUseCase: DeleteParticipantUseCase) {
  delete {
    val eventId = call.parameters["eventId"]?.let { UUID.fromString(it) }
      ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing eventId")

    val userEmail = call.parameters["userEmail"]
      ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing userEmail")

    Either.catch { call.authenticatedUser().email }
      .mapLeft { Exception("Authentication failed", it) }
      .flatMap { authenticatedEmail ->
        deleteParticipantUseCase.execute(userEmail, eventId, authenticatedEmail)
      }
      .fold(
        { it.handleFailure(call) },
        { call.respond(HttpStatusCode.NoContent) },
      )
  }
}

private suspend fun Exception.handleFailure(call: ApplicationCall) = when (this) {
  is ForbiddenParticipantDeleteException -> call.logAndRespond(
    status = HttpStatusCode.Forbidden,
    responseMessage = ClientErrorMessage.of(
      type = FORBIDDEN_ERROR_TYPE,
      detail = "Only the organizer can remove participants",
    ),
    failure = this,
  )

  is ParticipantNotFoundException -> call.logAndRespond(
    status = HttpStatusCode.NotFound,
    responseMessage = ClientErrorMessage.of(
      type = NOT_FOUND_ERROR_TYPE,
      detail = "Participant ${this.userEmail} not found for event ${this.eventId}",
    ),
    failure = this,
  )

  is DeleteParticipantException -> call.logAndRespond(
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
