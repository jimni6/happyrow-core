package com.happyrow.core.infrastructure.common.error

sealed class ValidationError(val msg: String) : Exception(msg) {
  object MissingValue : ValidationError("Missing value")
  class InvalidFormat(msg: String = "Invalid format value") : ValidationError(msg)
  data class InvalidParameter(val parameterName: String, val error: ValidationError) :
    ValidationError("Invalid parameter $parameterName: ${error.msg}")
}
