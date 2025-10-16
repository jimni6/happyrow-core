package com.happyrow.core.domain.resource.create.model

import com.happyrow.core.domain.resource.common.model.ResourceCategory
import java.util.UUID

data class CreateResourceRequest(
  val name: String,
  val category: ResourceCategory,
  val suggestedQuantity: Int,
  val eventId: UUID,
)
