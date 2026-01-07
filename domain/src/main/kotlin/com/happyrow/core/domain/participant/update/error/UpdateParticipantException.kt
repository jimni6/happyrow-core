package com.happyrow.core.domain.participant.update.error

import java.util.UUID

class UpdateParticipantException(
  userEmail: String,
  eventId: UUID,
  override val cause: Throwable,
) : Exception("Failed to update participant for user $userEmail and event $eventId", cause)
