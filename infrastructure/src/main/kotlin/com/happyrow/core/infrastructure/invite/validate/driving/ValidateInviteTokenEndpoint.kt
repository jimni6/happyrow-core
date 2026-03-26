package com.happyrow.core.infrastructure.invite.validate.driving

import com.happyrow.core.domain.invite.validate.ValidateInviteTokenUseCase
import com.happyrow.core.infrastructure.invite.validate.driving.dto.toDto
import com.happyrow.core.infrastructure.technical.ktor.ProblemDetail
import com.happyrow.core.infrastructure.technical.ktor.logAndRespond
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.validateInviteTokenEndpoint(validateInviteTokenUseCase: ValidateInviteTokenUseCase) {
  get {
    val token = call.parameters["token"]
      ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing token")

    validateInviteTokenUseCase.execute(token)
      .fold(
        { call.logAndRespond(problem = ProblemDetail.technicalError(), failure = it as? Exception) },
        { validation ->
          if (validation == null) {
            call.respond(
              HttpStatusCode.NotFound,
              ProblemDetail.of(HttpStatusCode.NotFound, "INVITE_NOT_FOUND", "This invitation link does not exist."),
            )
          } else {
            call.respond(HttpStatusCode.OK, validation.toDto())
          }
        },
      )
  }
}
