package com.happyrow.core.domain.event.create.error

import java.util.UUID

data class GetEventException(
  val identifier: UUID,
  override val cause: Throwable,
) : Exception("Failed to get event with id: $identifier", cause)
