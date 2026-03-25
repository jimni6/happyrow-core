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
) {
  init {
    require(name.isNotBlank()) { "Resource name must not be blank" }
    require(initialQuantity in 1..MAX_QUANTITY) { "Initial quantity must be between 1 and $MAX_QUANTITY" }
    require(suggestedQuantity in 0..MAX_QUANTITY) { "Suggested quantity must be between 0 and $MAX_QUANTITY" }
  }

  companion object {
    const val MAX_QUANTITY = 10_000
  }
}
