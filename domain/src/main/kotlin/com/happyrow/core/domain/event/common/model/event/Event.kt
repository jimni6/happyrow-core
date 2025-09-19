package com.happyrow.core.domain.event.common.model.event

import com.happyrow.core.domain.event.creator.model.Creator
import java.time.Instant
import java.util.UUID

data class Event(
  val identifier: UUID,
  val name: String,
  val description: String,
  val eventDate: Instant,
  val creationDate: Instant,
  val updateDate: Instant,
  val creator: Creator,
  val location: String,
  val type: EventType,
  val members: List<Creator> = listOf(),
)
