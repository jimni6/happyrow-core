package com.happyrow.core.domain.participant.delete.error

import java.util.UUID

class ForbiddenParticipantDeleteException(
  val authenticatedEmail: String,
  val targetEmail: String,
  val eventId: UUID,
) : Exception(
  "User $authenticatedEmail is not authorized to delete participant $targetEmail from event $eventId",
)
