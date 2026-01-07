package com.happyrow.core.domain.participant.common.error

import com.happyrow.core.domain.participant.create.model.CreateParticipantRequest

class CreateParticipantRepositoryException(
  val request: CreateParticipantRequest,
  override val cause: Throwable,
) : Exception("Failed to create participant for user ${request.userEmail} and event ${request.eventId}", cause)
