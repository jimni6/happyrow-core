package com.happyrow.core.domain.participant.get.error

import java.util.UUID

class GetParticipantsException(
  val eventId: UUID,
  override val cause: Throwable,
) : Exception("Failed to get participants for event $eventId", cause)
