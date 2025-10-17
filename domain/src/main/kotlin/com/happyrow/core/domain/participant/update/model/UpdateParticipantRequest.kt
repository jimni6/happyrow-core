package com.happyrow.core.domain.participant.update.model

import com.happyrow.core.domain.participant.common.model.ParticipantStatus
import java.util.UUID

data class UpdateParticipantRequest(
  val userId: UUID,
  val eventId: UUID,
  val status: ParticipantStatus,
)
