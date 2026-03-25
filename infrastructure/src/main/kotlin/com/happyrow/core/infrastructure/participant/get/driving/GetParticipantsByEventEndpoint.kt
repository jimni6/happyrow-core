package com.happyrow.core.infrastructure.participant.get.driving

import arrow.core.Either
import arrow.core.flatMap
import com.happyrow.core.domain.common.model.PageRequest
import com.happyrow.core.domain.event.common.EventAccessControl
import com.happyrow.core.domain.event.common.error.ForbiddenAccessException
import com.happyrow.core.domain.participant.get.GetParticipantsByEventUseCase
import com.happyrow.core.domain.participant.get.error.GetParticipantsException
import com.happyrow.core.infrastructure.common.error.BadRequestException
import com.happyrow.core.infrastructure.participant.common.dto.toDto
import com.happyrow.core.infrastructure.technical.auth.authenticatedUser
import com.happyrow.core.infrastructure.technical.ktor.ProblemDetail
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
      Pair(user.userId, eventId)
    }
      .mapLeft {
        BadRequestException.InvalidParameterException(
          "auth_or_eventId",
          it.message ?: "Authentication or eventId error",
        )
      }
      .flatMap { (userId, eventId) ->
        eventAccessControl.assertUserHasAccess(userId, eventId)
          .map { eventId }
      }
      .flatMap { eventId ->
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
        val size = call.request.queryParameters["size"]?.toIntOrNull() ?: PageRequest.DEFAULT_PAGE_SIZE
        getParticipantsByEventUseCase.execute(eventId, PageRequest(page, size))
      }
      .fold(
        { it.handleFailure(call) },
        { page ->
          call.response.headers.append("X-Page", page.page.toString())
          call.response.headers.append("X-Size", page.size.toString())
          call.response.headers.append("X-Total-Elements", page.totalElements.toString())
          call.response.headers.append("X-Total-Pages", page.totalPages.toString())
          call.respond(HttpStatusCode.OK, page.content.map { it.toDto() })
        },
      )
  }
}

private suspend fun Exception.handleFailure(call: ApplicationCall) = when (this) {
  is ForbiddenAccessException -> call.logAndRespond(
    problem = ProblemDetail.of(
      HttpStatusCode.Forbidden,
      FORBIDDEN_ERROR_TYPE,
      "You do not have access to this event",
    ),
    failure = this,
  )

  is GetParticipantsException -> call.logAndRespond(
    problem = ProblemDetail.technicalError(),
    failure = this,
  )

  else -> call.logAndRespond(
    problem = ProblemDetail.technicalError(),
    failure = this,
  )
}
