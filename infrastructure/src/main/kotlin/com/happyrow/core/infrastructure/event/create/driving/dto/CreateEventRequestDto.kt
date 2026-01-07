package com.happyrow.core.infrastructure.event.create.driving.dto

import com.happyrow.core.domain.event.common.model.event.EventType
import com.happyrow.core.domain.event.create.model.CreateEventRequest
import com.happyrow.core.domain.event.creator.model.Creator
import java.time.Instant

class CreateEventRequestDto(
  val name: String,
  val description: String,
  val eventDate: String,
  val location: String,
  val type: EventType,
  val members: List<Creator> = listOf(),
) {
  fun toDomain(creator: String, creatorId: java.util.UUID) = CreateEventRequest(
    name.trim(),
    description,
    Instant.parse(eventDate),
    Creator(creator),
    creatorId,
    location,
    type,
    members,
  )
}
