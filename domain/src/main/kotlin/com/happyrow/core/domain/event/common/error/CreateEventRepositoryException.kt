package com.happyrow.core.domain.event.common.error

import com.happyrow.core.domain.event.create.model.CreateEventRequest

data class CreateEventRepositoryException(
  val request: CreateEventRequest,
  override val cause: Throwable? = null,
) : Exception("Failed to create event with request: $request", cause)
