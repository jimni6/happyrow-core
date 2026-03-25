package com.happyrow.core.domain.contribution.reduce.model

import java.util.UUID

data class ReduceContributionRequest(
  val userEmail: String,
  val eventId: UUID,
  val resourceId: UUID,
  val quantity: Int,
) {
  init {
    require(quantity > 0) { "Reduction quantity must be positive" }
  }
}
