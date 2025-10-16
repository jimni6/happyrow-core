package com.happyrow.core.domain.event.get.error

import java.util.UUID

data class GetEventException(
  val identifier: UUID?,
  override val cause: Throwable,
) : Exception("Failed to get event${identifier?.let { " with id: $it" } ?: "s"}", cause)
