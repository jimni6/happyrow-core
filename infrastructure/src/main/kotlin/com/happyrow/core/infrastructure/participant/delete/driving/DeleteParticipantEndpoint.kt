package com.happyrow.core.infrastructure.participant.delete.driving

import arrow.core.Either
import arrow.core.flatMap
import com.happyrow.core.domain.participant.delete.DeleteParticipantUseCase
import com.happyrow.core.domain.participant.delete.error.DeleteParticipantException
import com.happyrow.core.domain.participant.delete.error.ForbiddenParticipantDeleteException
import com.happyrow.core.domain.participant.update.error.ParticipantNotFoundException
import com.happyrow.core.infrastructure.technical.auth.authenticatedUser
import com.happyrow.core.infrastructure.technical.ktor.ProblemDetail
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

    val userId = call.parameters["userId"]?.let { UUID.fromString(it) }
      ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing userId")

    Either.catch { call.authenticatedUser().userId }
      .mapLeft { Exception("Authentication failed", it) }
      .flatMap { authenticatedUserId ->
        deleteParticipantUseCase.execute(userId, eventId, authenticatedUserId)
      }
      .fold(
        { it.handleFailure(call) },
        { call.respond(HttpStatusCode.NoContent) },
      )
  }
}

private suspend fun Exception.handleFailure(call: ApplicationCall) = when (this) {
  is ForbiddenParticipantDeleteException -> call.logAndRespond(
    problem = ProblemDetail.of(
      HttpStatusCode.Forbidden,
      FORBIDDEN_ERROR_TYPE,
      "Only the organizer can remove participants",
    ),
    failure = this,
  )

  is ParticipantNotFoundException -> call.logAndRespond(
    problem = ProblemDetail.of(
      HttpStatusCode.NotFound,
      NOT_FOUND_ERROR_TYPE,
      "Participant ${this.userId} not found for event ${this.eventId}",
    ),
    failure = this,
  )

  is DeleteParticipantException -> call.logAndRespond(
    problem = ProblemDetail.technicalError(),
    failure = this,
  )

  else -> call.logAndRespond(
    problem = ProblemDetail.technicalError(),
    failure = this,
  )
}
