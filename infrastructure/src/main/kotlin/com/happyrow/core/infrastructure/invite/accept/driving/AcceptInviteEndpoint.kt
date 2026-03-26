package com.happyrow.core.infrastructure.invite.accept.driving

import arrow.core.Either
import arrow.core.flatMap
import com.happyrow.core.domain.invite.accept.AcceptInviteUseCase
import com.happyrow.core.domain.invite.accept.error.AcceptInviteException
import com.happyrow.core.infrastructure.invite.accept.driving.dto.toDto
import com.happyrow.core.infrastructure.technical.auth.authenticatedUser
import com.happyrow.core.infrastructure.technical.ktor.ProblemDetail
import com.happyrow.core.infrastructure.technical.ktor.logAndRespond
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import java.util.UUID

fun Route.acceptInviteEndpoint(acceptInviteUseCase: AcceptInviteUseCase) {
  post {
    val token = call.parameters["token"]
      ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing token")

    Either.catch {
      val user = call.authenticatedUser()
      Triple(token, UUID.fromString(user.userId), user.email)
    }
      .mapLeft { AcceptInviteException("Invalid authentication", it) }
      .flatMap { (inviteToken, userId, email) ->
        acceptInviteUseCase.execute(inviteToken, userId, email)
      }
      .map { it.toDto() }
      .fold(
        { it.handleAcceptFailure(call) },
        { call.respond(HttpStatusCode.OK, it) },
      )
  }
}

private suspend fun AcceptInviteException.handleAcceptFailure(call: ApplicationCall) = when {
  message == "INVITE_NOT_FOUND" -> call.logAndRespond(
    problem = ProblemDetail.of(HttpStatusCode.NotFound, "INVITE_NOT_FOUND", "This invitation link does not exist."),
    failure = this,
  )

  message == "INVITE_EXPIRED" -> call.logAndRespond(
    problem = ProblemDetail.of(
      HttpStatusCode.Gone,
      "INVITE_EXPIRED",
      "This invitation link has expired. Please ask the organizer for a new one.",
    ),
    failure = this,
  )

  message == "INVITE_REVOKED" -> call.logAndRespond(
    problem = ProblemDetail.of(
      HttpStatusCode.Gone,
      "INVITE_REVOKED",
      "This invitation link has been revoked by the organizer.",
    ),
    failure = this,
  )

  message == "INVITE_EXHAUSTED" -> call.logAndRespond(
    problem = ProblemDetail.of(
      HttpStatusCode.Gone,
      "INVITE_EXHAUSTED",
      "This invitation link has reached its maximum number of uses.",
    ),
    failure = this,
  )

  message?.startsWith("ALREADY_PARTICIPANT") == true -> {
    val eventId = message?.substringAfter("ALREADY_PARTICIPANT:")
    call.respond(
      HttpStatusCode.Conflict,
      mapOf(
        "error" to "ALREADY_PARTICIPANT",
        "message" to "You are already a participant of this event.",
        "event_id" to eventId,
      ),
    )
  }

  else -> call.logAndRespond(
    problem = ProblemDetail.technicalError(),
    failure = this,
  )
}
