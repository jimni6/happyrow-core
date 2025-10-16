package com.happyrow.core.domain.resource.get.error

import java.util.UUID

class GetResourcesException(
  val eventId: UUID,
  override val cause: Throwable,
) : Exception("Failed to get resources for event $eventId", cause)
