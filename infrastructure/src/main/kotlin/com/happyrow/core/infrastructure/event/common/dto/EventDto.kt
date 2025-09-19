package com.happyrow.core.infrastructure.event.common.dto

import com.happyrow.core.domain.event.common.model.event.Event
import com.happyrow.core.domain.event.common.model.event.EventType
import java.time.Instant
import java.util.UUID

data class EventDto(
  val identifier: UUID,
  val name: String,
  val description: String,
  val eventDate: Instant,
  val creationDate: Instant,
  val updateDate: Instant,
  val creator: String,
  val location: String,
  val type: EventType,
  val members: List<String> = listOf(),
)

fun Event.toDto() = EventDto(
  identifier,
  name,
  description,
  eventDate,
  creationDate,
  updateDate,
  creator.toString(),
  location,
  type,
  members.map { it.toString() },
)
