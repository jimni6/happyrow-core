package com.happyrow.core.infrastructure.resource.common.dto

import com.happyrow.core.domain.resource.common.model.Resource
import com.happyrow.core.domain.resource.common.model.ResourceWithContributors

data class ResourceDto(
  val identifier: String,
  val name: String,
  val category: String,
  val suggestedQuantity: Int,
  val currentQuantity: Int,
  val eventId: String,
  val contributors: List<ContributorDto>,
  val version: Int,
  val createdAt: Long,
  val updatedAt: Long,
)

fun Resource.toDto(contributors: List<ContributorDto> = emptyList()): ResourceDto = ResourceDto(
  identifier = this.identifier.toString(),
  name = this.name,
  category = this.category.name,
  suggestedQuantity = this.suggestedQuantity,
  currentQuantity = this.currentQuantity,
  eventId = this.eventId.toString(),
  contributors = contributors,
  version = this.version,
  createdAt = this.createdAt.toEpochMilli(),
  updatedAt = this.updatedAt.toEpochMilli(),
)

fun ResourceWithContributors.toDto(): ResourceDto = ResourceDto(
  identifier = this.resource.identifier.toString(),
  name = this.resource.name,
  category = this.resource.category.name,
  suggestedQuantity = this.resource.suggestedQuantity,
  currentQuantity = this.resource.currentQuantity,
  eventId = this.resource.eventId.toString(),
  contributors = this.contributors.map { contributor ->
    ContributorDto(
      userId = contributor.userId,
      quantity = contributor.quantity,
      contributedAt = contributor.contributedAt,
    )
  },
  version = this.resource.version,
  createdAt = this.resource.createdAt.toEpochMilli(),
  updatedAt = this.resource.updatedAt.toEpochMilli(),
)
