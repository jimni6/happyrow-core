package com.happyrow.core.domain.invite.common.model

import java.time.Instant
import java.util.UUID

data class AcceptInviteResult(
  val eventId: UUID,
  val userId: UUID,
  val userName: String?,
  val status: String,
  val joinedAt: Instant,
)
