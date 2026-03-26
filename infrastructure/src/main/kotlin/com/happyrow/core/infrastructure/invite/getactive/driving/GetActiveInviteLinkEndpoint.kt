package com.happyrow.core.infrastructure.invite.getactive.driving

import arrow.core.Either
import arrow.core.flatMap
import com.happyrow.core.domain.invite.getactive.ForbiddenInviteAccessException
import com.happyrow.core.domain.invite.getactive.GetActiveInviteLinkUseCase
import com.happyrow.core.infrastructure.invite.common.dto.toDto
import com.happyrow.core.infrastructure.technical.auth.authenticatedUser
import com.happyrow.core.infrastructure.technical.ktor.ProblemDetail
import com.happyrow.core.infrastructure.technical.ktor.logAndRespond
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import java.util.UUID

fun Route.getActiveInviteLinkEndpoint(getActiveInviteLinkUseCase: GetActiveInviteLinkUseCase) {
  get {
    val eventId = call.parameters["eventId"]?.let { UUID.fromString(it) }
      ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing eventId")

    Either.catch { call.authenticatedUser() }
      .mapLeft { it as Exception }
      .flatMap { user ->
        getActiveInviteLinkUseCase.execute(eventId, user.userId)
      }
      .fold(
        { it.handleGetActiveFailure(call) },
        { inviteLink ->
          if (inviteLink != null) {
            call.respond(HttpStatusCode.OK, inviteLink.toDto())
          } else {
            call.respond(HttpStatusCode.NoContent)
          }
        },
      )
  }
}

private suspend fun Exception.handleGetActiveFailure(call: ApplicationCall) = when (this) {
  is ForbiddenInviteAccessException -> call.logAndRespond(
    problem = ProblemDetail.of(HttpStatusCode.Forbidden, "FORBIDDEN", "You are not the organizer of this event"),
    failure = this,
  )

  else -> call.logAndRespond(
    problem = ProblemDetail.technicalError(),
    failure = this,
  )
}
