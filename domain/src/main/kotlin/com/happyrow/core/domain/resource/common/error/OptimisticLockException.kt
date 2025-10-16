package com.happyrow.core.domain.resource.common.error

import java.util.UUID

class OptimisticLockException(
  val resourceId: UUID,
  val expectedVersion: Int,
) : Exception(
  "Optimistic lock failure for resource $resourceId with version $expectedVersion. " +
    "Resource was modified by another user.",
)
