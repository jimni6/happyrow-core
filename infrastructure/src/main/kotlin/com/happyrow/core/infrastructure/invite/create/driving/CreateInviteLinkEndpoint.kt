package com.happyrow.core.infrastructure.invite.create.driving

import arrow.core.Either
import arrow.core.flatMap
import com.happyrow.core.domain.invite.create.CreateInviteLinkUseCase
import com.happyrow.core.domain.invite.create.error.CreateInviteLinkException
import com.happyrow.core.infrastructure.common.error.BadRequestException
import com.happyrow.core.infrastructure.invite.common.dto.toDto
import com.happyrow.core.infrastructure.invite.create.driving.dto.CreateInviteLinkRequestDto
import com.happyrow.core.infrastructure.technical.auth.authenticatedUser
import com.happyrow.core.infrastructure.technical.ktor.ProblemDetail
import com.happyrow.core.infrastructure.technical.ktor.logAndRespond
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import java.util.UUID

fun Route.createInviteLinkEndpoint(createInviteLinkUseCase: CreateInviteLinkUseCase) {
  post {
    val eventId = call.parameters["eventId"]?.let { UUID.fromString(it) }
      ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing eventId")

    Either.catch {
      val user = call.authenticatedUser()
      val requestDto = call.receive<CreateInviteLinkRequestDto>()
      requestDto.toDomain(eventId, UUID.fromString(user.userId))
    }
      .mapLeft { BadRequestException.InvalidBodyException(it) }
      .flatMap { request -> createInviteLinkUseCase.execute(request) }
      .map { it.toDto() }
      .fold(
        { it.handleCreateFailure(call) },
        { call.respond(HttpStatusCode.Created, it) },
      )
  }
}

private suspend fun Exception.handleCreateFailure(call: ApplicationCall) = when {
  this is BadRequestException -> call.logAndRespond(
    problem = ProblemDetail.of(HttpStatusCode.BadRequest, type, message),
    failure = this,
  )

  this is CreateInviteLinkException && message?.contains("not found") == true -> call.logAndRespond(
    problem = ProblemDetail.of(HttpStatusCode.NotFound, "NOT_FOUND", message ?: "Event not found"),
    failure = this,
  )

  this is CreateInviteLinkException && message?.contains("organizer") == true -> call.logAndRespond(
    problem = ProblemDetail.of(HttpStatusCode.Forbidden, "FORBIDDEN", message ?: "Not the organizer"),
    failure = this,
  )

  this is CreateInviteLinkException && message?.contains("already exists") == true -> call.logAndRespond(
    problem = ProblemDetail.of(HttpStatusCode.Conflict, "CONFLICT", message ?: "Active invite already exists"),
    failure = this,
  )

  else -> call.logAndRespond(
    problem = ProblemDetail.technicalError(),
    failure = this,
  )
}
