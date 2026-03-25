package com.happyrow.core.infrastructure.participant

import com.happyrow.core.domain.event.common.EventAccessControl
import com.happyrow.core.domain.participant.create.CreateParticipantUseCase
import com.happyrow.core.domain.participant.delete.DeleteParticipantUseCase
import com.happyrow.core.domain.participant.get.GetParticipantsByEventUseCase
import com.happyrow.core.domain.participant.update.UpdateParticipantUseCase
import com.happyrow.core.infrastructure.participant.create.driving.createParticipantEndpoint
import com.happyrow.core.infrastructure.participant.delete.driving.deleteParticipantEndpoint
import com.happyrow.core.infrastructure.participant.get.driving.getParticipantsByEventEndpoint
import com.happyrow.core.infrastructure.participant.update.driving.updateParticipantEndpoint
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.routing.Route
import io.ktor.server.routing.route

fun Route.participantEndpoints(
  createParticipantUseCase: CreateParticipantUseCase,
  getParticipantsByEventUseCase: GetParticipantsByEventUseCase,
  updateParticipantUseCase: UpdateParticipantUseCase,
  deleteParticipantUseCase: DeleteParticipantUseCase,
  eventAccessControl: EventAccessControl,
) = route("/events/{eventId}/participants") {
  getParticipantsByEventEndpoint(getParticipantsByEventUseCase, eventAccessControl)
  rateLimit(RateLimitName("mutation")) {
    createParticipantEndpoint(createParticipantUseCase, eventAccessControl)
    route("/{userEmail}") {
      updateParticipantEndpoint(updateParticipantUseCase)
      deleteParticipantEndpoint(deleteParticipantUseCase)
    }
  }
}
