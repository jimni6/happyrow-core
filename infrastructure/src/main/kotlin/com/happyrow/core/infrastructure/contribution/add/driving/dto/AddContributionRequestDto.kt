package com.happyrow.core.infrastructure.contribution.add.driving.dto

import com.happyrow.core.domain.contribution.add.model.AddContributionRequest
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class AddContributionRequestDto(
  val quantity: Int,
)

fun AddContributionRequestDto.toDomain(userId: UUID, eventId: UUID, resourceId: UUID): AddContributionRequest =
  AddContributionRequest(
    userId = userId,
    eventId = eventId,
    resourceId = resourceId,
    quantity = this.quantity,
  )
