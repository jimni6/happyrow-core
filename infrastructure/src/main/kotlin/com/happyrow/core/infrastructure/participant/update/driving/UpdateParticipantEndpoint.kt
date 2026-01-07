package com.happyrow.core.infrastructure.participant.update.driving

import arrow.core.Either
import arrow.core.flatMap
import com.happyrow.core.domain.participant.update.UpdateParticipantUseCase
import com.happyrow.core.domain.participant.update.error.UpdateParticipantException
import com.happyrow.core.infrastructure.event.common.error.BadRequestException
import com.happyrow.core.infrastructure.participant.common.dto.toDto
import com.happyrow.core.infrastructure.participant.update.driving.dto.UpdateParticipantRequestDto
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

fun Route.updateParticipantEndpoint(updateParticipantUseCase: UpdateParticipantUseCase) {
  put {
    val eventId = call.parameters["eventId"]?.let { UUID.fromString(it) }
      ?: return@put call.respond(HttpStatusCode.BadRequest, "Missing eventId")

    val userId = call.parameters["userId"]?.let { UUID.fromString(it) }
      ?: return@put call.respond(HttpStatusCode.BadRequest, "Missing userId")

    Either.catch {
      call.authenticatedUser()
      call.receive<UpdateParticipantRequestDto>()
    }
      .mapLeft { BadRequestException.InvalidBodyException(it) }
      .map { it.toDomain(userId, eventId) }
      .flatMap { request -> updateParticipantUseCase.execute(request) }
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

  is UpdateParticipantException -> call.logAndRespond(
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
