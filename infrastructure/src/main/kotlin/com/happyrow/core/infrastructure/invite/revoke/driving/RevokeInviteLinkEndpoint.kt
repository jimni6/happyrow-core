package com.happyrow.core.infrastructure.invite.revoke.driving

import arrow.core.Either
import arrow.core.flatMap
import com.happyrow.core.domain.invite.revoke.RevokeInviteLinkUseCase
import com.happyrow.core.domain.invite.revoke.error.RevokeInviteLinkException
import com.happyrow.core.infrastructure.technical.auth.authenticatedUser
import com.happyrow.core.infrastructure.technical.ktor.ProblemDetail
import com.happyrow.core.infrastructure.technical.ktor.logAndRespond
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import java.util.UUID

fun Route.revokeInviteLinkEndpoint(revokeInviteLinkUseCase: RevokeInviteLinkUseCase) {
  delete {
    val eventId = call.parameters["eventId"]?.let { UUID.fromString(it) }
      ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing eventId")
    val token = call.parameters["token"]
      ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing token")

    Either.catch { call.authenticatedUser() }
      .mapLeft { RevokeInviteLinkException("Authentication failed", it) }
      .flatMap { user ->
        revokeInviteLinkUseCase.execute(eventId, token, user.userId)
      }
      .fold(
        { it.handleRevokeFailure(call) },
        { call.respond(HttpStatusCode.NoContent) },
      )
  }
}

private suspend fun RevokeInviteLinkException.handleRevokeFailure(call: ApplicationCall) = when (message) {
  "FORBIDDEN" -> call.logAndRespond(
    problem = ProblemDetail.of(HttpStatusCode.Forbidden, "FORBIDDEN", "You are not the organizer of this event"),
    failure = this,
  )

  "NOT_FOUND" -> call.logAndRespond(
    problem = ProblemDetail.of(HttpStatusCode.NotFound, "NOT_FOUND", "Invite link not found"),
    failure = this,
  )

  "ALREADY_REVOKED" -> call.logAndRespond(
    problem = ProblemDetail.of(HttpStatusCode.Conflict, "ALREADY_REVOKED", "This invite link is already revoked"),
    failure = this,
  )

  else -> call.logAndRespond(
    problem = ProblemDetail.technicalError(),
    failure = this,
  )
}
