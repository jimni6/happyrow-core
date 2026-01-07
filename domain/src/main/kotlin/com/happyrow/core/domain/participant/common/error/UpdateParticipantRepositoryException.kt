package com.happyrow.core.domain.participant.common.error

import java.util.UUID

class UpdateParticipantRepositoryException(
  val userId: UUID,
  val eventId: UUID,
  override val cause: Throwable,
) : Exception("Failed to update participant for user $userId and event $eventId", cause)
