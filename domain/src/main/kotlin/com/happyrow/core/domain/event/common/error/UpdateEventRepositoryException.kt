package com.happyrow.core.domain.event.common.error

import com.happyrow.core.domain.event.update.model.UpdateEventRequest

class UpdateEventRepositoryException(
  val request: UpdateEventRequest,
  override val cause: Throwable?,
) : Exception("Failed to update event with identifier ${request.identifier}", cause)
