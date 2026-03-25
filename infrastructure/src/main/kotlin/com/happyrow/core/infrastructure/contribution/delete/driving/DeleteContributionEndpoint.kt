package com.happyrow.core.infrastructure.contribution.delete.driving

import arrow.core.Either
import arrow.core.flatMap
import com.happyrow.core.domain.contribution.delete.DeleteContributionUseCase
import com.happyrow.core.domain.contribution.delete.error.DeleteContributionException
import com.happyrow.core.infrastructure.common.error.BadRequestException
import com.happyrow.core.infrastructure.technical.auth.authenticatedUser
import com.happyrow.core.infrastructure.technical.ktor.ProblemDetail
import com.happyrow.core.infrastructure.technical.ktor.logAndRespond
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import java.util.UUID

fun Route.deleteContributionEndpoint(deleteContributionUseCase: DeleteContributionUseCase) {
  delete {
    val eventId = call.parameters["eventId"]?.let { UUID.fromString(it) }
      ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing eventId")

    val resourceId = call.parameters["resourceId"]?.let { UUID.fromString(it) }
      ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing resourceId")

    Either.catch {
      val user = call.authenticatedUser()
      UUID.fromString(user.userId)
    }
      .mapLeft { BadRequestException.InvalidBodyException(it) }
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
    problem = ProblemDetail.of(HttpStatusCode.BadRequest, type = type, detail = message),
    failure = this,
  )

  is DeleteContributionException -> call.logAndRespond(
    problem = ProblemDetail.technicalError(),
    failure = this,
  )

  else -> call.logAndRespond(
    problem = ProblemDetail.technicalError(),
    failure = this,
  )
}
