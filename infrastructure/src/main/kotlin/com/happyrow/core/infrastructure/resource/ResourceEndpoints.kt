package com.happyrow.core.infrastructure.resource

import com.happyrow.core.domain.resource.create.CreateResourceUseCase
import com.happyrow.core.domain.resource.get.GetResourcesByEventUseCase
import com.happyrow.core.infrastructure.resource.create.driving.createResourceEndpoint
import com.happyrow.core.infrastructure.resource.get.driving.getResourcesByEventEndpoint
import io.ktor.server.routing.Route
import io.ktor.server.routing.route

fun Route.resourceEndpoints(
  createResourceUseCase: CreateResourceUseCase,
  getResourcesByEventUseCase: GetResourcesByEventUseCase,
) = route("/events/{eventId}/resources") {
  createResourceEndpoint(createResourceUseCase)
  getResourcesByEventEndpoint(getResourcesByEventUseCase)
}
