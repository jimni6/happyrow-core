package com.happyrow.core.domain.event.creator.model

import java.util.UUID

@JvmInline
value class Creator(val value: UUID) {
  override fun toString(): String = value.toString()
}
