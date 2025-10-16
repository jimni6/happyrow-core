package com.happyrow.core.domain.event.delete.error

import java.util.UUID

class DeleteEventException(
  val identifier: UUID,
  override val cause: Throwable,
) : Exception("Failed to delete event with identifier $identifier", cause)
