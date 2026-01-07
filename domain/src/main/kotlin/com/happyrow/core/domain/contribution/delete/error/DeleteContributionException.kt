package com.happyrow.core.domain.contribution.delete.error

import java.util.UUID

class DeleteContributionException(
  val userEmail: String,
  val resourceId: UUID,
  override val cause: Throwable,
) : Exception("Failed to delete contribution for resource $resourceId by user $userEmail", cause)
