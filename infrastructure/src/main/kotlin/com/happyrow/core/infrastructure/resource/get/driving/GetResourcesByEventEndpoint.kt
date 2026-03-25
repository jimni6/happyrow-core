package com.happyrow.core.infrastructure.resource.get.driving

import arrow.core.Either
import arrow.core.flatMap
import com.happyrow.core.domain.common.model.PageRequest
import com.happyrow.core.domain.event.common.EventAccessControl
import com.happyrow.core.domain.event.common.error.ForbiddenAccessException
import com.happyrow.core.domain.resource.get.GetResourcesByEventUseCase
import com.happyrow.core.domain.resource.get.error.GetResourcesException
import com.happyrow.core.infrastructure.common.error.BadRequestException
import com.happyrow.core.infrastructure.resource.common.dto.toDto
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

fun Route.getResourcesByEventEndpoint(
  getResourcesByEventUseCase: GetResourcesByEventUseCase,
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
        getResourcesByEventUseCase.execute(eventId, PageRequest(page, size))
      }
      .map { page ->
        mapOf(
          "content" to page.content.map { it.toDto() },
          "page" to page.page,
          "size" to page.size,
          "totalElements" to page.totalElements,
          "totalPages" to page.totalPages,
        )
      }
      .fold(
        { it.handleFailure(call) },
        { call.respond(HttpStatusCode.OK, it) },
      )
  }
}

private suspend fun Exception.handleFailure(call: ApplicationCall) = when (this) {
  is ForbiddenAccessException -> call.logAndRespond(
    problem = ProblemDetail.of(
      HttpStatusCode.Forbidden,
      type = FORBIDDEN_ERROR_TYPE,
      detail = "You do not have access to this event",
    ),
    failure = this,
  )

  is GetResourcesException -> call.logAndRespond(
    problem = ProblemDetail.technicalError(),
    failure = this,
  )

  else -> call.logAndRespond(
    problem = ProblemDetail.technicalError(),
    failure = this,
  )
}
