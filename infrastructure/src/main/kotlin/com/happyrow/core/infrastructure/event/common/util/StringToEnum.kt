package com.happyrow.core.infrastructure.event.common.util

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.happyrow.core.domain.event.common.model.event.EventType
import com.happyrow.core.infrastructure.event.common.error.EnumParsingException

fun String.toEventType(): Either<EnumParsingException, EventType> = EventType.values()
  .firstOrNull { enum -> enum.name == this }?.right()
  ?: EnumParsingException("No AudienceType matches value '$this'", this).left()
