package com.happyrow.core.infrastructure.resource.create.driving.dto

import com.happyrow.core.domain.resource.common.model.ResourceCategory
import com.happyrow.core.domain.resource.create.model.CreateResourceRequest
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class CreateResourceRequestDto(
  val name: String,
  val category: String,
  val suggestedQuantity: Int,
)

fun CreateResourceRequestDto.toDomain(eventId: UUID): CreateResourceRequest = CreateResourceRequest(
  name = this.name,
  category = ResourceCategory.valueOf(this.category),
  suggestedQuantity = this.suggestedQuantity,
  eventId = eventId,
)
