package com.happyrow.core.infrastructure.resource.common.dto

import com.happyrow.core.domain.resource.common.model.Resource
import kotlinx.serialization.Serializable

@Serializable
data class ResourceDto(
  val identifier: String,
  val name: String,
  val category: String,
  val suggestedQuantity: Int,
  val currentQuantity: Int,
  val eventId: String,
  val version: Int,
  val createdAt: Long,
  val updatedAt: Long,
)

fun Resource.toDto(): ResourceDto = ResourceDto(
  identifier = this.identifier.toString(),
  name = this.name,
  category = this.category.name,
  suggestedQuantity = this.suggestedQuantity,
  currentQuantity = this.currentQuantity,
  eventId = this.eventId.toString(),
  version = this.version,
  createdAt = this.createdAt.toEpochMilli(),
  updatedAt = this.updatedAt.toEpochMilli(),
)
