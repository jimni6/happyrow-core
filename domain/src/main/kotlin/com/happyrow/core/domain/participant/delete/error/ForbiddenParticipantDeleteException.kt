package com.happyrow.core.domain.participant.delete.error

import java.util.UUID

class ForbiddenParticipantDeleteException(
  val authenticatedUserId: String,
  val targetUserId: UUID,
  val eventId: UUID,
) : Exception(
  "User $authenticatedUserId is not authorized to delete participant $targetUserId from event $eventId",
)
