package com.happyrow.core.infrastructure.event.delete.error

import java.util.UUID

class ForeignKeyConstraintException(
  val eventId: UUID,
  cause: Throwable?,
) : Exception("Cannot delete event $eventId because it has related resources or participants", cause)
