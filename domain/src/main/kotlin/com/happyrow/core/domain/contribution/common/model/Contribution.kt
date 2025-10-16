package com.happyrow.core.domain.contribution.common.model

import java.time.Instant
import java.util.UUID

data class Contribution(
  val identifier: UUID,
  val participantId: UUID,
  val resourceId: UUID,
  val quantity: Int,
  val createdAt: Instant,
  val updatedAt: Instant,
)
