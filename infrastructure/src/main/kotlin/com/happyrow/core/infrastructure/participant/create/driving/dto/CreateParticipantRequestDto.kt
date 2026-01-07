package com.happyrow.core.infrastructure.participant.create.driving.dto

import com.happyrow.core.domain.participant.common.model.ParticipantStatus
import com.happyrow.core.domain.participant.create.model.CreateParticipantRequest
import java.util.UUID

class CreateParticipantRequestDto(
  val userEmail: String,
  val status: String = "CONFIRMED",
) {
  fun toDomain(eventId: UUID): CreateParticipantRequest = CreateParticipantRequest(
    userEmail = this.userEmail,
    eventId = eventId,
    status = ParticipantStatus.valueOf(this.status),
  )
}
