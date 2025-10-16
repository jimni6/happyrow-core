package com.happyrow.core.domain.contribution.common.error

import java.util.UUID

class ContributionRepositoryException(
  val resourceId: UUID?,
  override val cause: Throwable?,
) : Exception("Failed to manage contribution for resource $resourceId", cause)
