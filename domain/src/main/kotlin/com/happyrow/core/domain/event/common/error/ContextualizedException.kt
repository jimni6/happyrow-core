package com.happyrow.core.domain.event.common.error

open class ContextualizedException(
  message: String? = null,
  cause: Throwable? = null,
  open val context: Map<String, String> = emptyMap(),
) : Exception(message, cause)
