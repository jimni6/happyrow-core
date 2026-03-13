package com.happyrow.core.domain.participant.create.model

import com.happyrow.core.domain.participant.common.model.ParticipantStatus
import java.util.UUID

data class CreateParticipantRequest(
  val userEmail: String,
  val userName: String? = null,
  val eventId: UUID,
  val status: ParticipantStatus = ParticipantStatus.CONFIRMED,
)
