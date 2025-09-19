package com.happyrow.core.infrastructure.event.create.error

data class UnicityConflictException(
  override val message: String,
  override val cause: Throwable? = null,
) : Exception(message)
