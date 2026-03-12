package com.happyrow.core.domain.participant.update.error

import java.util.UUID

class ForbiddenParticipantUpdateException(
  val authenticatedEmail: String,
  val targetEmail: String,
  val eventId: UUID,
) : Exception(
  "User $authenticatedEmail is not authorized to update participant $targetEmail for event $eventId",
)
