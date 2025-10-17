package com.happyrow.core.infrastructure.contribution.delete.driving

import arrow.core.flatMap
import com.happyrow.core.domain.contribution.delete.DeleteContributionUseCase
import com.happyrow.core.domain.contribution.delete.error.DeleteContributionException
import com.happyrow.core.infrastructure.event.common.error.BadRequestException
import com.happyrow.core.infrastructure.technical.ktor.ClientErrorMessage
import com.happyrow.core.infrastructure.technical.ktor.ClientErrorMessage.Companion.technicalErrorMessage
import com.happyrow.core.infrastructure.technical.ktor.getHeader
import com.happyrow.core.infrastructure.technical.ktor.logAndRespond
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import java.util.UUID

private const val USER_ID_HEADER = "x-user-id"

fun Route.deleteContributionEndpoint(deleteContributionUseCase: DeleteContributionUseCase) {
  delete {
    val eventId = call.parameters["eventId"]?.let { UUID.fromString(it) }
      ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing eventId")

    val resourceId = call.parameters["resourceId"]?.let { UUID.fromString(it) }
      ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing resourceId")

    call.getHeader(USER_ID_HEADER)
      .map { UUID.fromString(it) }
      .flatMap { userId ->
        deleteContributionUseCase.execute(userId, eventId, resourceId)
      }
      .fold(
        { it.handleFailure(call) },
        { call.respond(HttpStatusCode.NoContent) },
      )
  }
}

private suspend fun Exception.handleFailure(call: ApplicationCall) = when (this) {
  is BadRequestException -> call.logAndRespond(
    status = HttpStatusCode.BadRequest,
    responseMessage = ClientErrorMessage.of(type = type, detail = message),
    failure = this,
  )

  is DeleteContributionException -> call.logAndRespond(
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
