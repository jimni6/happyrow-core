package com.happyrow.core.infrastructure.resource.create.driving

import arrow.core.Either
import arrow.core.flatMap
import com.happyrow.core.domain.resource.create.CreateResourceUseCase
import com.happyrow.core.domain.resource.create.error.CreateResourceException
import com.happyrow.core.infrastructure.event.common.error.BadRequestException
import com.happyrow.core.infrastructure.resource.common.dto.toDto
import com.happyrow.core.infrastructure.resource.create.driving.dto.CreateResourceRequestDto
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
import java.util.UUID

fun Route.createResourceEndpoint(createResourceUseCase: CreateResourceUseCase) {
  post {
    val eventId = call.parameters["eventId"]?.let { UUID.fromString(it) }
      ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing eventId")

    Either.catch {
      val user = call.authenticatedUser()
      val requestDto = call.receive<CreateResourceRequestDto>()
      requestDto.toDomain(eventId, user.email)
    }
      .mapLeft { BadRequestException.InvalidBodyException(it) }
      .flatMap { request -> createResourceUseCase.execute(request) }
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

  is CreateResourceException -> call.logAndRespond(
    status = HttpStatusCode.InternalServerError,
    responseMessage = technicalErrorMessage(),
    failure = this,
  )

  else -> call.logAndRespond(
    status = HttpStatusCode.InternalServerError,
    responseMessage = technicalErrorMessage(),
    failure = this,
  )
}
