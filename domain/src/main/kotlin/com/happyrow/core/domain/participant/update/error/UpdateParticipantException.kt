package com.happyrow.core.domain.participant.update.error

class UpdateParticipantException(
  userEmail: String,
  eventId: java.util.UUID,
  override val cause: Throwable,
) : Exception("Failed to update participant for user $userEmail and event $eventId", cause)
