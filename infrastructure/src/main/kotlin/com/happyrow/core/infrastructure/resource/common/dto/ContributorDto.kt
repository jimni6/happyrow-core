package com.happyrow.core.infrastructure.resource.common.dto

data class ContributorDto(
  val userId: String,
  val quantity: Int,
  val contributedAt: Long,
)
