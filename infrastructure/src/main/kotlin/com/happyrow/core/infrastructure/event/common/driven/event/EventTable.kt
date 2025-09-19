package com.happyrow.core.infrastructure.event.common.driven.event

import arrow.core.Either
import arrow.core.getOrElse
import com.happyrow.core.domain.event.common.model.event.Event
import com.happyrow.core.domain.event.common.model.event.EventType
import com.happyrow.core.domain.event.creator.model.Creator
import com.happyrow.core.infrastructure.event.common.error.EntityParsingException
import com.happyrow.core.infrastructure.event.common.util.toEventType
import com.happyrow.core.infrastructure.technical.exposed.PGEnum
import com.happyrow.core.infrastructure.technical.exposed.array
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.UUIDColumnType
import org.jetbrains.exposed.sql.javatime.timestamp
import java.util.UUID

private const val VARCHAR_LENGTH = 256

object EventTable : UUIDTable("configuration.event", "identifier") {
  val name: Column<String> = varchar("name", VARCHAR_LENGTH)
  val description: Column<String> = varchar("description", VARCHAR_LENGTH)
  val eventDate = timestamp("event_date")
  val creator = varchar("creator", VARCHAR_LENGTH)
  val location: Column<String> = varchar("location", VARCHAR_LENGTH)
  val type: Column<EventType> = customEnumeration(
    name = "type",
    sql = "EVENT_TYPE",
    fromDb = { value -> (value as String).toEventType().getOrElse { throw it } },
    toDb = { PGEnum("EVENT_TYPE", it) },
  )
  val creationDate = timestamp("creation_date")
  val updateDate = timestamp("update_date")
  val members = array("members", UUIDColumnType(), UUID::class) { UUID.fromString(it.toString()) }
}

fun ResultRow.toEvent(): Either<EntityParsingException, Event> = Either.catch {
  Event(
    identifier = this[EventTable.id].value,
    name = this[EventTable.name],
    description = this[EventTable.description],
    eventDate = this[EventTable.eventDate],
    creationDate = this[EventTable.creationDate],
    updateDate = this[EventTable.updateDate],
    creator = Creator(this[EventTable.creator]),
    location = this[EventTable.location],
    type = this[EventTable.type],
    members = this[EventTable.members].map { Creator(it.toString()) },
  )
}
  .mapLeft { EntityParsingException(Event::class, it) }
