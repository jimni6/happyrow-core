package com.happyrow.core.domain.participant.update.error

import java.util.UUID

class UpdateParticipantException(
  userId: UUID,
  eventId: UUID,
  override val cause: Throwable,
) : Exception("Failed to update participant for user $userId and event $eventId", cause)
