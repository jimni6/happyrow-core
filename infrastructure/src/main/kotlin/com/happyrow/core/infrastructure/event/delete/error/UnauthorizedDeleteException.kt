package com.happyrow.core.infrastructure.event.delete.error

import java.util.UUID

class UnauthorizedDeleteException(
  val eventId: UUID,
  val userId: String,
) : Exception("User $userId is not authorized to delete event $eventId")
