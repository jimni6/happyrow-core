package com.happyrow.core.domain.event.update.model

import com.happyrow.core.domain.event.common.model.event.EventType
import com.happyrow.core.domain.event.creator.model.Creator
import java.time.Instant
import java.util.UUID

data class UpdateEventRequest(
  val identifier: UUID,
  val name: String,
  val description: String,
  val eventDate: Instant,
  val location: String,
  val type: EventType,
  val updater: Creator,
  val members: List<Creator> = listOf(),
)
