package com.happyrow.core.infrastructure.invite.create.driving.dto

import com.happyrow.core.domain.invite.create.model.CreateInviteLinkRequest
import java.util.UUID

class CreateInviteLinkRequestDto(
  val expiresInDays: Int? = null,
  val maxUses: Int? = null,
) {
  fun toDomain(eventId: UUID, userId: UUID): CreateInviteLinkRequest = CreateInviteLinkRequest(
    eventId = eventId,
    createdBy = userId,
    expiresInDays = expiresInDays ?: CreateInviteLinkRequest.DEFAULT_EXPIRES_IN_DAYS,
    maxUses = maxUses,
  )
}
