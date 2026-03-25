package com.happyrow.core.domain.participant.delete.error

import java.util.UUID

class ForbiddenParticipantDeleteException(
  val authenticatedUserId: String,
  val targetEmail: String,
  val eventId: UUID,
) : Exception(
  "User $authenticatedUserId is not authorized to delete participant $targetEmail from event $eventId",
)
