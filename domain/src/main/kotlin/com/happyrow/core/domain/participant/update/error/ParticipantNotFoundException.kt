package com.happyrow.core.domain.participant.update.error

import java.util.UUID

class ParticipantNotFoundException(
  val userId: UUID,
  val eventId: UUID,
) : Exception("Participant $userId not found for event $eventId")
