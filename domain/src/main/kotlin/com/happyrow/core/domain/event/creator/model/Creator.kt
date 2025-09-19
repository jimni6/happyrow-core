package com.happyrow.core.domain.event.creator.model

@JvmInline
value class Creator(private val value: String) {
  override fun toString(): String {
    return value
  }
}
