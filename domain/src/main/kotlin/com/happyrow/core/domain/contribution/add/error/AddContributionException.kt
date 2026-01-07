package com.happyrow.core.domain.contribution.add.error

import com.happyrow.core.domain.contribution.add.model.AddContributionRequest

class AddContributionException(
  val request: AddContributionRequest,
  override val cause: Throwable,
) : Exception(
  "Failed to add contribution for resource ${request.resourceId} by user ${request.userId}",
  cause,
)
