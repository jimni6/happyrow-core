package com.happyrow.core.infrastructure.resource.common.driven

import com.happyrow.core.infrastructure.event.common.driven.event.EventTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp

private const val RESOURCE_NAME_MAX_LENGTH = 255
private const val RESOURCE_CATEGORY_MAX_LENGTH = 50

object ResourceTable : UUIDTable("configuration.resource", "id") {
  val name = varchar("name", RESOURCE_NAME_MAX_LENGTH)
  val category = varchar("category", RESOURCE_CATEGORY_MAX_LENGTH)
  val suggestedQuantity = integer("suggested_quantity").default(0)
  val currentQuantity = integer("current_quantity").default(0)
  val eventId = uuid("event_id").references(EventTable.id)
  val version = integer("version").default(1)
  val createdAt = timestamp("created_at")
  val updatedAt = timestamp("updated_at")

  init {
    index("idx_resource_event", false, eventId)
  }
}
