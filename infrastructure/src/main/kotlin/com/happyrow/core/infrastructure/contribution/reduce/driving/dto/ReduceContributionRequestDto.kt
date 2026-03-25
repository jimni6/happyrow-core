package com.happyrow.core.infrastructure.contribution.reduce.driving.dto

import com.happyrow.core.domain.contribution.reduce.model.ReduceContributionRequest
import java.util.UUID

class ReduceContributionRequestDto(
  val quantity: Int,
) {
  fun toDomain(userId: UUID, eventId: UUID, resourceId: UUID): ReduceContributionRequest = ReduceContributionRequest(
    userId = userId,
    eventId = eventId,
    resourceId = resourceId,
    quantity = this.quantity,
  )
}
