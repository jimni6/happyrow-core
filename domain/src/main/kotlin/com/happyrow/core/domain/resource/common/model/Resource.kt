package com.happyrow.core.domain.resource.common.model

import java.time.Instant
import java.util.UUID

data class Resource(
  val identifier: UUID,
  val name: String,
  val category: ResourceCategory,
  val suggestedQuantity: Int,
  val currentQuantity: Int,
  val eventId: UUID,
  val version: Int,
  val createdAt: Instant,
  val updatedAt: Instant,
)
