package com.happyrow.core.infrastructure.resource.common.driven

import com.happyrow.core.infrastructure.event.common.driven.event.EventTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

private const val RESOURCE_NAME_MAX_LENGTH = 255

object ResourceTable : Table("configuration.resource") {
  val id = uuid("id").autoGenerate()
  val name = varchar("name", RESOURCE_NAME_MAX_LENGTH)
  val category = customEnumeration(
    "category",
    "RESOURCE_CATEGORY",
    { value -> ResourceCategoryDb.valueOf(value as String) },
    { it.name },
  )
  val suggestedQuantity = integer("suggested_quantity").default(0)
  val currentQuantity = integer("current_quantity").default(0)
  val eventId = uuid("event_id").references(EventTable.id)
  val version = integer("version").default(1)
  val createdAt = timestamp("created_at")
  val updatedAt = timestamp("updated_at")

  override val primaryKey = PrimaryKey(id)

  init {
    index("idx_resource_event", false, eventId)
  }
}

enum class ResourceCategoryDb {
  FOOD,
  DRINK,
  UTENSIL,
  DECORATION,
  OTHER,
}
