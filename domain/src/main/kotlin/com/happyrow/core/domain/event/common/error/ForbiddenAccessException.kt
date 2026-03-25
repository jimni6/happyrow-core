package com.happyrow.core.domain.event.common.error

import java.util.UUID

class ForbiddenAccessException(
  val userId: String,
  val eventId: UUID,
) : Exception("User $userId does not have access to event $eventId")
