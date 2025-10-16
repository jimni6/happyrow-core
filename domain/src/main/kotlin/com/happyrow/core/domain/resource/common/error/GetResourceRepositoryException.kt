package com.happyrow.core.domain.resource.common.error

import java.util.UUID

class GetResourceRepositoryException(
  val eventId: UUID?,
  override val cause: Throwable?,
) : Exception("Failed to get resources for event $eventId", cause)
