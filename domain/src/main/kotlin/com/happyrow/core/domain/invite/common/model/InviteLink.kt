package com.happyrow.core.domain.invite.common.model

import java.time.Instant
import java.util.UUID

data class InviteLink(
  val id: UUID,
  val token: String,
  val eventId: UUID,
  val createdBy: UUID,
  val status: InviteStatus,
  val maxUses: Int?,
  val currentUses: Int,
  val createdAt: Instant,
  val expiresAt: Instant,
)
