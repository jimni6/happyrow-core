package com.happyrow.core.domain.event.create.model

import com.happyrow.core.domain.event.common.model.event.EventType
import com.happyrow.core.domain.event.creator.model.Creator
import java.time.Instant

data class CreateEventRequest(
  val name: String,
  val description: String,
  val eventDate: Instant,
  val creator: Creator,
  val location: String,
  val type: EventType,
  val members: List<Creator> = listOf(),
)
