package com.happyrow.core.infrastructure.event.create.driving

import arrow.core.Either
import arrow.core.flatMap
import com.happyrow.core.domain.event.create.CreateEventUseCase
import com.happyrow.core.domain.event.create.error.CreateEventException
import com.happyrow.core.domain.event.create.error.CreateEventRepositoryException
import com.happyrow.core.infrastructure.event.common.dto.toDto
import com.happyrow.core.infrastructure.event.common.error.BadRequestException
import com.happyrow.core.infrastructure.event.create.driving.dto.CreateEventRequestDto
import com.happyrow.core.infrastructure.event.create.error.UnicityConflictException
import com.happyrow.core.infrastructure.technical.auth.authenticatedUser
import com.happyrow.core.infrastructure.technical.ktor.ClientErrorMessage
import com.happyrow.core.infrastructure.technical.ktor.ClientErrorMessage.Companion.technicalErrorMessage
import com.happyrow.core.infrastructure.technical.ktor.logAndRespond
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

private const val NAME_ALREADY_EXISTS_ERROR_TYPE = "NAME_ALREADY_EXISTS"

fun Route.createEventEndpoint(createEventUseCase: CreateEventUseCase) = route("") {
  post {
    Either.catch {
      val user = call.authenticatedUser()
      val requestDto = call.receive<CreateEventRequestDto>()
      requestDto.toDomain(user.userId, java.util.UUID.fromString(user.userId))
    }
      .mapLeft { BadRequestException.InvalidBodyException(it) }
      .flatMap { request -> createEventUseCase.create(request) }
      .map { it.toDto() }
      .fold(
        { it.handleFailure(call) },
        { call.respond(HttpStatusCode.Created, it) },
      )
  }
}

private suspend fun Exception.handleFailure(call: ApplicationCall) = when (this) {
  is BadRequestException -> call.logAndRespond(
    status = HttpStatusCode.BadRequest,
    responseMessage = ClientErrorMessage.of(type = type, detail = message),
    failure = this,
  )

  is CreateEventException -> this.handleFailure(call)

  else -> call.logAndRespond(
    status = HttpStatusCode.InternalServerError,
    responseMessage = technicalErrorMessage(),
    failure = this,
  )
}

private suspend fun CreateEventException.handleFailure(call: ApplicationCall) = when (cause) {
  is CreateEventRepositoryException -> (cause as CreateEventRepositoryException).handleFailure(call)

  else -> call.logAndRespond(
    status = HttpStatusCode.InternalServerError,
    responseMessage = technicalErrorMessage(),
    failure = this,
  )
}

private suspend fun CreateEventRepositoryException.handleFailure(call: ApplicationCall) = when (cause) {
  is UnicityConflictException -> call.logAndRespond(
    status = HttpStatusCode.Conflict,
    responseMessage = ClientErrorMessage.of(
      type = NAME_ALREADY_EXISTS_ERROR_TYPE,
      detail = request.name,
    ),
    failure = this,
  )

  else -> call.logAndRespond(
    status = HttpStatusCode.InternalServerError,
    responseMessage = technicalErrorMessage(),
    failure = this,
  )
}
