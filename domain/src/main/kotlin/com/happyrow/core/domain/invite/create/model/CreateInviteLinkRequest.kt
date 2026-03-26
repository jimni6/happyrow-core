package com.happyrow.core.domain.invite.create.model

import java.util.UUID

data class CreateInviteLinkRequest(
  val eventId: UUID,
  val createdBy: UUID,
  val expiresInDays: Int = DEFAULT_EXPIRES_IN_DAYS,
  val maxUses: Int? = null,
) {
  init {
    require(expiresInDays in MIN_EXPIRES_IN_DAYS..MAX_EXPIRES_IN_DAYS) {
      "expiresInDays must be between $MIN_EXPIRES_IN_DAYS and $MAX_EXPIRES_IN_DAYS"
    }
    require(maxUses == null || maxUses > 0) { "maxUses must be positive or null" }
  }

  companion object {
    const val DEFAULT_EXPIRES_IN_DAYS = 7
    const val MIN_EXPIRES_IN_DAYS = 1
    const val MAX_EXPIRES_IN_DAYS = 30
  }
}
