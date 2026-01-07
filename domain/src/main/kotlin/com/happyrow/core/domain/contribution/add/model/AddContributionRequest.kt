package com.happyrow.core.domain.contribution.add.model

import java.util.UUID

data class AddContributionRequest(
  val userEmail: String,
  val eventId: UUID,
  val resourceId: UUID,
  val quantity: Int,
)
