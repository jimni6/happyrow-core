package com.happyrow.core.infrastructure.event.common.error

data class EnumParsingException(override val message: String, val value: String) : ValidationError(message)
