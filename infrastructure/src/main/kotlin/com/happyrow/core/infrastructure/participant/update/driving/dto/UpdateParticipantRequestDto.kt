package com.happyrow.core.infrastructure.participant.update.driving.dto

import com.happyrow.core.domain.participant.common.model.ParticipantStatus
import com.happyrow.core.domain.participant.update.model.UpdateParticipantRequest
import java.util.UUID

class UpdateParticipantRequestDto(
  val status: String,
) {
  fun toDomain(userEmail: String, eventId: UUID): UpdateParticipantRequest = UpdateParticipantRequest(
    userEmail = userEmail,
    eventId = eventId,
    status = ParticipantStatus.valueOf(this.status),
  )
}
