package com.happyrow.core

import com.happyrow.core.domain.contribution.add.AddContributionUseCase
import com.happyrow.core.domain.contribution.delete.DeleteContributionUseCase
import com.happyrow.core.domain.event.create.CreateEventUseCase
import com.happyrow.core.domain.event.delete.DeleteEventUseCase
import com.happyrow.core.domain.event.get.GetEventsByOrganizerUseCase
import com.happyrow.core.domain.event.update.UpdateEventUseCase
import com.happyrow.core.domain.resource.create.CreateResourceUseCase
import com.happyrow.core.domain.resource.get.GetResourcesByEventUseCase
import com.happyrow.core.infrastructure.contribution.contributionEndpoints
import com.happyrow.core.infrastructure.event.eventEndpoints
import com.happyrow.core.infrastructure.resource.resourceEndpoints
import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject

const val BASE_PATH = "/event/configuration"

fun Application.configureRouting() {
  val createEventUseCase: CreateEventUseCase by inject()
  val getEventsByOrganizerUseCase: GetEventsByOrganizerUseCase by inject()
  val updateEventUseCase: UpdateEventUseCase by inject()
  val deleteEventUseCase: DeleteEventUseCase by inject()
  val createResourceUseCase: CreateResourceUseCase by inject()
  val getResourcesByEventUseCase: GetResourcesByEventUseCase by inject()
  val addContributionUseCase: AddContributionUseCase by inject()
  val deleteContributionUseCase: DeleteContributionUseCase by inject()

  routing {
    route(BASE_PATH) {
      route("/api/v1") {
        eventEndpoints(createEventUseCase, getEventsByOrganizerUseCase, updateEventUseCase, deleteEventUseCase)
        resourceEndpoints(createResourceUseCase, getResourcesByEventUseCase)
        contributionEndpoints(addContributionUseCase, deleteContributionUseCase)
      }
    }

    get("/") {
      call.respondText("Hello from happyrow-core! 🎉", ContentType.Text.Plain)
    }
//    get("/health") {
//      handleHealthCheck(call)
//    }

    get("/info") {
      call.respond(
        mapOf(
          "name" to "happyrow-core",
          "version" to "1.0.0",
          "environment" to (System.getenv("ENVIRONMENT") ?: "unknown"),
          "timestamp" to System.currentTimeMillis(),
        ),
      )
    }
  }
}

//private suspend fun handleHealthCheck(call: ApplicationCall) {
//  val dataSource by inject<DataSource>()
//  try {
//    // Test database connection
//    dataSource.connection.use { connection ->
//      val isValid = connection.isValid(DB_CONNECTION_TIMEOUT_SECONDS)
//      if (isValid) {
//        call.respond(
//          HttpStatusCode.OK,
//          mapOf(
//            "status" to "healthy",
//            "database" to "connected",
//            "timestamp" to System.currentTimeMillis(),
//          ),
//        )
//      } else {
//        call.respond(
//          HttpStatusCode.ServiceUnavailable,
//          mapOf(
//            "status" to "unhealthy",
//            "database" to "connection_invalid",
//            "timestamp" to System.currentTimeMillis(),
//          ),
//        )
//      }
//    }
//  } catch (e: SQLException) {
//    call.respond(
//      HttpStatusCode.ServiceUnavailable,
//      mapOf(
//        "status" to "unhealthy",
//        "database" to "connection_failed",
//        "error" to e.message,
//        "timestamp" to System.currentTimeMillis(),
//      ),
//    )
//  }
//}
