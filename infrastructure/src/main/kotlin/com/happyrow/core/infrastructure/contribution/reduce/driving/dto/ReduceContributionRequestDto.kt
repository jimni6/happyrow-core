package com.happyrow.core.infrastructure.contribution.reduce.driving.dto

import com.happyrow.core.domain.contribution.reduce.model.ReduceContributionRequest
import java.util.UUID

data class ReduceContributionRequestDto(
  val quantity: Int,
) {
  fun toDomain(userEmail: String, eventId: UUID, resourceId: UUID) = ReduceContributionRequest(
    userEmail = userEmail,
    eventId = eventId,
    resourceId = resourceId,
    quantity = quantity,
  )
}
