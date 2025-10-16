package com.happyrow.core.infrastructure.event

import com.happyrow.core.domain.event.create.CreateEventUseCase
import com.happyrow.core.domain.event.get.GetEventsByOrganizerUseCase
import com.happyrow.core.infrastructure.event.create.driving.createEventEndpoint
import com.happyrow.core.infrastructure.event.get.driving.getEventsEndpoint
import io.ktor.server.routing.Route
import io.ktor.server.routing.route

const val CREATOR_HEADER = "x-user-id"

fun Route.eventEndpoints(
  createEventUseCase: CreateEventUseCase,
  getEventsByOrganizerUseCase: GetEventsByOrganizerUseCase,
) = route("/events") {
  createEventEndpoint(createEventUseCase)
  getEventsEndpoint(getEventsByOrganizerUseCase)
}
