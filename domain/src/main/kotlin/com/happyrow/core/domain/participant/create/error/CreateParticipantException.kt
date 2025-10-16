package com.happyrow.core.domain.participant.create.error

import com.happyrow.core.domain.participant.create.model.CreateParticipantRequest

class CreateParticipantException(
  val request: CreateParticipantRequest,
  override val cause: Throwable,
) : Exception("Failed to create participant for user ${request.userId} and event ${request.eventId}", cause)
