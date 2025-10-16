package com.happyrow.core.domain.participant.create.model

import com.happyrow.core.domain.participant.common.model.ParticipantStatus
import java.util.UUID

data class CreateParticipantRequest(
  val userId: UUID,
  val eventId: UUID,
  val status: ParticipantStatus = ParticipantStatus.CONFIRMED,
)
