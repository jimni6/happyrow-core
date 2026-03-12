package com.happyrow.core.domain.participant.update.error

import java.util.UUID

class ParticipantNotFoundException(
  val userEmail: String,
  val eventId: UUID,
) : Exception("Participant $userEmail not found for event $eventId")
