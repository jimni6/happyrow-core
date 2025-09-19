package com.happyrow.core.domain.event.create.error

import com.happyrow.core.domain.event.create.model.CreateEventRequest

data class CreateEventException(
  val request: CreateEventRequest,
  override val cause: Throwable? = null,
) : Exception("Failed to create event with request: $request", cause)
