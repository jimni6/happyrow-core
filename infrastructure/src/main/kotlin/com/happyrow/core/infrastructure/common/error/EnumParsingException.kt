package com.happyrow.core.infrastructure.common.error

data class EnumParsingException(override val message: String, val value: String) : ValidationError(message)
