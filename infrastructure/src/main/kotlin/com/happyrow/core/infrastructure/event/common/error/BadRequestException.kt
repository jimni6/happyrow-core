package com.happyrow.core.infrastructure.event.common.error

sealed class BadRequestException(override val message: String, val type: String = "BAD_REQUEST") : Exception(message) {

  data class IllegalUUIDException(val value: String) :
    BadRequestException("Cannot convert $value to UUID", "INVALID_UUID")

  data class MissingHeaderException(
    private val name: String,
    override val message: String = "Missing header $name",
  ) : BadRequestException(message, "MISSING_HEADER")

  data class MissingParameterException(
    private val name: String,
    override val message: String = "Missing parameter $name",
  ) : BadRequestException(message, "MISSING_PARAMETER")

  data class InvalidParameterException(
    private val name: String,
    private val value: String,
    override val message: String = "Value is invalid for parameter $name: $value",
  ) : BadRequestException(message, "INVALID_PARAMETER")

  data class InvalidBodyException(
    override val cause: Throwable,
  ) : BadRequestException("Failed to parse body", "INVALID_BODY")
}
