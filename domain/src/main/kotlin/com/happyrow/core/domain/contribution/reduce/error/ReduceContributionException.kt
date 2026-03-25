package com.happyrow.core.domain.contribution.reduce.error

import com.happyrow.core.domain.contribution.reduce.model.ReduceContributionRequest

data class ReduceContributionException(
  val request: ReduceContributionRequest,
  override val cause: Throwable,
) : Exception("Failed to reduce contribution for user ${request.userId} on resource ${request.resourceId}", cause)
