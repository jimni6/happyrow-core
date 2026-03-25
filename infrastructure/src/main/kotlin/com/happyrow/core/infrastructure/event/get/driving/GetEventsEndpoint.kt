package com.happyrow.core.infrastructure.event.get.driving

import arrow.core.Either
import arrow.core.flatMap
import com.happyrow.core.domain.common.model.PageRequest
import com.happyrow.core.domain.event.get.GetEventsByUserUseCase
import com.happyrow.core.domain.event.get.error.GetEventException
import com.happyrow.core.infrastructure.event.common.dto.toDto
import com.happyrow.core.infrastructure.technical.auth.authenticatedUser
import com.happyrow.core.infrastructure.technical.ktor.ProblemDetail
import com.happyrow.core.infrastructure.technical.ktor.logAndRespond
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

private const val INVALID_ORGANIZER_ID_ERROR_TYPE = "INVALID_ORGANIZER_ID"

fun Route.getEventsEndpoint(getEventsByUserUseCase: GetEventsByUserUseCase) {
  get {
    Either.catch {
      val user = call.authenticatedUser()
      val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
      val size = call.request.queryParameters["size"]?.toIntOrNull() ?: PageRequest.DEFAULT_PAGE_SIZE
      Triple(user, page, size)
    }
      .mapLeft { InvalidOrganizerIdException("authenticated_user", it) }
      .flatMap { (user, page, size) ->
        val pageRequest = PageRequest(page, size)
        getEventsByUserUseCase.execute(user.userId, user.email, pageRequest)
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
  is InvalidOrganizerIdException -> call.logAndRespond(
    problem = ProblemDetail.of(
      HttpStatusCode.BadRequest,
      type = INVALID_ORGANIZER_ID_ERROR_TYPE,
      detail = "Invalid organizerId: $organizerId",
    ),
    failure = this,
  )

  is GetEventException -> call.logAndRespond(
    problem = ProblemDetail.technicalError(),
    failure = this,
  )

  else -> call.logAndRespond(
    problem = ProblemDetail.technicalError(),
    failure = this,
  )
}

private class InvalidOrganizerIdException(val organizerId: String, cause: Throwable) : Exception(cause)
