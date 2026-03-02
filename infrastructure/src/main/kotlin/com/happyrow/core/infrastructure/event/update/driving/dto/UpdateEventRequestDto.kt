package com.happyrow.core.infrastructure.event.update.driving.dto

import com.happyrow.core.domain.event.common.model.event.EventType
import com.happyrow.core.domain.event.creator.model.Creator
import com.happyrow.core.domain.event.update.model.UpdateEventRequest
import java.time.Instant
import java.util.UUID

class UpdateEventRequestDto(
  val name: String,
  val description: String,
  val eventDate: String,
  val location: String,
  val type: EventType,
  val members: List<Creator> = listOf(),
) {
  fun toDomain(identifier: UUID, updater: String): UpdateEventRequest {
    require(name.isNotBlank()) { "Event name must not be blank" }
    require(location.isNotBlank()) { "Event location must not be blank" }
    return UpdateEventRequest(
      identifier = identifier,
      name = name.trim(),
      description = description,
      eventDate = Instant.parse(eventDate),
      location = location.trim(),
      type = type,
      updater = Creator(updater),
      members = members,
    )
  }
}
