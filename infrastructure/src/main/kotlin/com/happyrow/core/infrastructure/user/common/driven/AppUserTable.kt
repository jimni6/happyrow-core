package com.happyrow.core.infrastructure.user.common.driven

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp

private const val EMAIL_MAX_LENGTH = 255
private const val NAME_MAX_LENGTH = 255

object AppUserTable : UUIDTable("configuration.app_user", "id") {
  val email = varchar("email", EMAIL_MAX_LENGTH)
  val name = varchar("name", NAME_MAX_LENGTH).nullable().default(null)
  val createdAt = timestamp("created_at")
  val updatedAt = timestamp("updated_at")

  init {
    uniqueIndex("uq_app_user_email", email)
  }
}
