package com.happyrow.core.infrastructure.event.update.driving

import arrow.core.Either
import arrow.core.flatMap
import com.happyrow.core.domain.event.common.error.EventNotFoundException
import com.happyrow.core.domain.event.common.error.UpdateEventRepositoryException
import com.happyrow.core.domain.event.update.UpdateEventUseCase
import com.happyrow.core.domain.event.update.error.UpdateEventException
import com.happyrow.core.infrastructure.event.common.dto.toDto
import com.happyrow.core.infrastructure.event.common.error.BadRequestException
import com.happyrow.core.infrastructure.event.create.error.UnicityConflictException
import com.happyrow.core.infrastructure.event.update.driving.dto.UpdateEventRequestDto
import com.happyrow.core.infrastructure.technical.auth.authenticatedUser
import com.happyrow.core.infrastructure.technical.ktor.ClientErrorMessage
import com.happyrow.core.infrastructure.technical.ktor.ClientErrorMessage.Companion.technicalErrorMessage
import com.happyrow.core.infrastructure.technical.ktor.logAndRespond
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.put
import java.util.UUID

private const val NAME_ALREADY_EXISTS_ERROR_TYPE = "NAME_ALREADY_EXISTS"
private const val EVENT_NOT_FOUND_ERROR_TYPE = "EVENT_NOT_FOUND"

fun Route.updateEventEndpoint(updateEventUseCase: UpdateEventUseCase) {
  put("/{id}") {
    val eventId = Either.catch {
      UUID.fromString(call.parameters["id"])
    }.mapLeft { BadRequestException.InvalidParameterException("id", call.parameters["id"] ?: "null") }

    eventId.flatMap { id ->
      Either.catch {
        val user = call.authenticatedUser()
        val requestDto = call.receive<UpdateEventRequestDto>()
        requestDto.toDomain(id, user.userId)
      }
        .mapLeft { BadRequestException.InvalidBodyException(it) }
    }
      .flatMap { request -> updateEventUseCase.update(request) }
      .map { it.toDto() }
      .fold(
        { it.handleFailure(call) },
        { call.respond(HttpStatusCode.OK, it) },
      )
  }
}

private suspend fun Exception.handleFailure(call: ApplicationCall) = when (this) {
  is BadRequestException -> call.logAndRespond(
    status = HttpStatusCode.BadRequest,
    responseMessage = ClientErrorMessage.of(type = type, detail = message),
    failure = this,
  )

  is UpdateEventException -> this.handleFailure(call)

  else -> call.logAndRespond(
    status = HttpStatusCode.InternalServerError,
    responseMessage = technicalErrorMessage(),
    failure = this,
  )
}

private suspend fun UpdateEventException.handleFailure(call: ApplicationCall) = when (cause) {
  is UpdateEventRepositoryException -> (cause as UpdateEventRepositoryException).handleFailure(call)

  else -> call.logAndRespond(
    status = HttpStatusCode.InternalServerError,
    responseMessage = technicalErrorMessage(),
    failure = this,
  )
}

private suspend fun UpdateEventRepositoryException.handleFailure(call: ApplicationCall) = when (cause) {
  is UnicityConflictException -> call.logAndRespond(
    status = HttpStatusCode.Conflict,
    responseMessage = ClientErrorMessage.of(
      type = NAME_ALREADY_EXISTS_ERROR_TYPE,
      detail = request.name,
    ),
    failure = this,
  )

  is EventNotFoundException -> call.logAndRespond(
    status = HttpStatusCode.NotFound,
    responseMessage = ClientErrorMessage.of(
      type = EVENT_NOT_FOUND_ERROR_TYPE,
      detail = "Event with id ${request.identifier} not found",
    ),
    failure = this,
  )

  else -> call.logAndRespond(
    status = HttpStatusCode.InternalServerError,
    responseMessage = technicalErrorMessage(),
    failure = this,
  )
}
