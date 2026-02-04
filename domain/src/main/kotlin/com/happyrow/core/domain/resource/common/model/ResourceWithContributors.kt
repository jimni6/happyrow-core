package com.happyrow.core.domain.resource.common.model

data class ResourceWithContributors(
  val resource: Resource,
  val contributors: List<ContributorInfo>,
)

data class ContributorInfo(
  val userId: String,
  val quantity: Int,
  val contributedAt: Long,
)
