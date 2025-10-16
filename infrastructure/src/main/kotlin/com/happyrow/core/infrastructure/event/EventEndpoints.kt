package com.happyrow.core.infrastructure.event

import com.happyrow.core.domain.event.create.CreateEventUseCase
import com.happyrow.core.domain.event.delete.DeleteEventUseCase
import com.happyrow.core.domain.event.get.GetEventsByOrganizerUseCase
import com.happyrow.core.domain.event.update.UpdateEventUseCase
import com.happyrow.core.infrastructure.event.create.driving.createEventEndpoint
import com.happyrow.core.infrastructure.event.delete.driving.deleteEventEndpoint
import com.happyrow.core.infrastructure.event.get.driving.getEventsEndpoint
import com.happyrow.core.infrastructure.event.update.driving.updateEventEndpoint
import io.ktor.server.routing.Route
import io.ktor.server.routing.route

const val CREATOR_HEADER = "x-user-id"

fun Route.eventEndpoints(
  createEventUseCase: CreateEventUseCase,
  getEventsByOrganizerUseCase: GetEventsByOrganizerUseCase,
  updateEventUseCase: UpdateEventUseCase,
  deleteEventUseCase: DeleteEventUseCase,
) = route("/events") {
  createEventEndpoint(createEventUseCase)
  getEventsEndpoint(getEventsByOrganizerUseCase)
  updateEventEndpoint(updateEventUseCase)
  deleteEventEndpoint(deleteEventUseCase)
}
