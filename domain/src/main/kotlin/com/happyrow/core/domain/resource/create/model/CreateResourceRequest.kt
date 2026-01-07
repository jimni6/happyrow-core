package com.happyrow.core.domain.resource.create.model

import com.happyrow.core.domain.resource.common.model.ResourceCategory
import java.util.UUID

data class CreateResourceRequest(
  val name: String,
  val category: ResourceCategory,
  val initialQuantity: Int,
  val eventId: UUID,
  val userEmail: String,
  val suggestedQuantity: Int = 0,
)
