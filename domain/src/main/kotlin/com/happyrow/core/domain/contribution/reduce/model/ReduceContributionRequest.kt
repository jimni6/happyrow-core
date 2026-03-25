package com.happyrow.core.domain.contribution.reduce.model

import java.util.UUID

data class ReduceContributionRequest(
  val userEmail: String,
  val eventId: UUID,
  val resourceId: UUID,
  val quantity: Int,
) {
  init {
    require(quantity in 1..MAX_QUANTITY) { "Reduction quantity must be between 1 and $MAX_QUANTITY" }
  }

  companion object {
    const val MAX_QUANTITY = 10_000
  }
}
