package com.happyrow.core.domain.resource.common.error

import java.util.UUID

class ResourceNotFoundException(
  val identifier: UUID,
) : Exception("Resource with id $identifier not found")
