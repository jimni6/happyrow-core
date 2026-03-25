package com.happyrow.core.domain.user.common.model

import java.time.Instant
import java.util.UUID

data class AppUser(
  val id: UUID,
  val email: String,
  val name: String? = null,
  val createdAt: Instant,
  val updatedAt: Instant,
)
