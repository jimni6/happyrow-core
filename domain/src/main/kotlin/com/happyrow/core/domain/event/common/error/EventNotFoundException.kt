package com.happyrow.core.domain.event.common.error

import java.util.UUID

data class EventNotFoundException(
  val eventIdentifier: UUID,
) : Exception("Event with event identifier $eventIdentifier not found")
