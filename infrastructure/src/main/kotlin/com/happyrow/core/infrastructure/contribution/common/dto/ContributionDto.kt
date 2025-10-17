package com.happyrow.core.infrastructure.contribution.common.dto

import com.happyrow.core.domain.contribution.common.model.Contribution

data class ContributionDto(
  val identifier: String,
  val participantId: String,
  val resourceId: String,
  val quantity: Int,
  val createdAt: Long,
  val updatedAt: Long,
)

fun Contribution.toDto(): ContributionDto = ContributionDto(
  identifier = this.identifier.toString(),
  participantId = this.participantId.toString(),
  resourceId = this.resourceId.toString(),
  quantity = this.quantity,
  createdAt = this.createdAt.toEpochMilli(),
  updatedAt = this.updatedAt.toEpochMilli(),
)
