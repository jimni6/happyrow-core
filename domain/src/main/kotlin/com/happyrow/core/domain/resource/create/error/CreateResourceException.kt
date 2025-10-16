package com.happyrow.core.domain.resource.create.error

import com.happyrow.core.domain.resource.create.model.CreateResourceRequest

class CreateResourceException(
  val request: CreateResourceRequest,
  override val cause: Throwable,
) : Exception("Failed to create resource '${request.name}' for event ${request.eventId}", cause)
