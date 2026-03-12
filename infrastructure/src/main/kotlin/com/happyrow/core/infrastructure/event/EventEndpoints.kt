package com.happyrow.core.infrastructure.event

import com.happyrow.core.domain.event.create.CreateEventUseCase
import com.happyrow.core.domain.event.delete.DeleteEventUseCase
import com.happyrow.core.domain.event.get.GetEventsByUserUseCase
import com.happyrow.core.domain.event.update.UpdateEventUseCase
import com.happyrow.core.infrastructure.event.create.driving.createEventEndpoint
import com.happyrow.core.infrastructure.event.delete.driving.deleteEventEndpoint
import com.happyrow.core.infrastructure.event.get.driving.getEventsEndpoint
import com.happyrow.core.infrastructure.event.update.driving.updateEventEndpoint
import io.ktor.server.routing.Route
import io.ktor.server.routing.route

fun Route.eventEndpoints(
  createEventUseCase: CreateEventUseCase,
  getEventsByUserUseCase: GetEventsByUserUseCase,
  updateEventUseCase: UpdateEventUseCase,
  deleteEventUseCase: DeleteEventUseCase,
) = route("/events") {
  createEventEndpoint(createEventUseCase)
  getEventsEndpoint(getEventsByUserUseCase)
  updateEventEndpoint(updateEventUseCase)
  deleteEventEndpoint(deleteEventUseCase)
}
