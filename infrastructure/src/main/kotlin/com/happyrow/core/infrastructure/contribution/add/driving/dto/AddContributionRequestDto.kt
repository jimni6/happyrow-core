package com.happyrow.core.infrastructure.contribution.add.driving.dto

import com.happyrow.core.domain.contribution.add.model.AddContributionRequest
import java.util.UUID

class AddContributionRequestDto(
  val quantity: Int,
) {
  fun toDomain(userEmail: String, eventId: UUID, resourceId: UUID): AddContributionRequest = AddContributionRequest(
    userEmail = userEmail,
    eventId = eventId,
    resourceId = resourceId,
    quantity = this.quantity,
  )
}
