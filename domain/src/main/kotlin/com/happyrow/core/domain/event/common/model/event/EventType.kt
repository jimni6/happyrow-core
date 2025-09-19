package com.happyrow.core.domain.event.common.model.event

enum class EventType(val code: String) {
  PARTY("party"),
  BIRTHDAY("birthday"),
  DINER("diner"),
  SNACK("snack"),
}

fun String.toEventType() = requireNotNull(EventType.entries.find { it.code == this }) {
  "Unknown '$this' value for EventType enum"
}
