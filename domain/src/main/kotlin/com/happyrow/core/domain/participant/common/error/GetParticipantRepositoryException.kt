package com.happyrow.core.domain.participant.common.error

import java.util.UUID

class GetParticipantRepositoryException(
  val eventId: UUID,
  override val cause: Throwable?,
) : Exception("Failed to get participants for event $eventId", cause)
