package com.happyrow.core.infrastructure.event.create.driving.dto

import com.happyrow.core.domain.event.common.model.event.EventType
import com.happyrow.core.domain.event.create.model.CreateEventRequest
import com.happyrow.core.domain.event.creator.model.Creator
import java.time.Instant
import java.util.UUID

class CreateEventRequestDto(
  val name: String,
  val description: String,
  val eventDate: String,
  val location: String,
  val type: EventType,
  val members: List<String> = listOf(),
) {
  fun toDomain(creatorId: String, creatorEmail: String): CreateEventRequest {
    require(name.isNotBlank()) { "Event name must not be blank" }
    require(location.isNotBlank()) { "Event location must not be blank" }
    return CreateEventRequest(
      name = name.trim(),
      description = description,
      eventDate = Instant.parse(eventDate),
      creator = Creator(UUID.fromString(creatorId)),
      creatorEmail = creatorEmail,
      location = location.trim(),
      type = type,
      members = members.map { Creator(UUID.fromString(it)) },
    )
  }
}
