package com.happyrow.core.infrastructure.participant

import com.happyrow.core.domain.participant.create.CreateParticipantUseCase
import com.happyrow.core.domain.participant.get.GetParticipantsByEventUseCase
import com.happyrow.core.domain.participant.update.UpdateParticipantUseCase
import com.happyrow.core.infrastructure.participant.create.driving.createParticipantEndpoint
import com.happyrow.core.infrastructure.participant.get.driving.getParticipantsByEventEndpoint
import com.happyrow.core.infrastructure.participant.update.driving.updateParticipantEndpoint
import io.ktor.server.routing.Route
import io.ktor.server.routing.route

fun Route.participantEndpoints(
  createParticipantUseCase: CreateParticipantUseCase,
  getParticipantsByEventUseCase: GetParticipantsByEventUseCase,
  updateParticipantUseCase: UpdateParticipantUseCase,
) = route("/events/{eventId}/participants") {
  createParticipantEndpoint(createParticipantUseCase)
  getParticipantsByEventEndpoint(getParticipantsByEventUseCase)
  route("/{userId}") {
    updateParticipantEndpoint(updateParticipantUseCase)
  }
}
