package com.happyrow.core.infrastructure.common.driven.event

import arrow.core.Either
import arrow.core.flatMap
import com.happyrow.core.domain.event.common.driven.event.EventRepository
import com.happyrow.core.domain.event.common.error.CreateEventRepositoryException
import com.happyrow.core.domain.event.common.error.EventNotFoundException
import com.happyrow.core.domain.event.common.model.event.Event
import com.happyrow.core.domain.event.create.error.GetEventException
import com.happyrow.core.domain.event.create.model.CreateEventRequest
import com.happyrow.core.infrastructure.event.common.driven.event.EventTable
import com.happyrow.core.infrastructure.event.common.driven.event.toEvent
import com.happyrow.core.infrastructure.event.create.error.UnicityConflictException
import com.happyrow.core.infrastructure.technical.config.ExposedDatabase
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Clock
import java.util.UUID

private const val SQL_UNIQUE_VIOLATION_CODE = "23505"

class SqlEventRepository(
  private val clock: Clock,
  private val exposedDatabase: ExposedDatabase,
) : EventRepository {
  override fun create(request: CreateEventRequest): Either<CreateEventRepositoryException, Event> = Either.Companion
    .catch {
      transaction(exposedDatabase.database) {
        EventTable.insertAndGetId {
          it[name] = request.name
          it[description] = request.description
          it[eventDate] = request.eventDate
          it[creator] = request.creator.toString()
          it[location] = request.location
          it[type] = request.type
          it[creationDate] = clock.instant()
          it[updateDate] = clock.instant()
          it[members] = request.members.map { UUID.fromString(it.toString()) }
        }.value
      }
    }
    .flatMap { find(it) }
    .mapLeft {
      when {
        it.isUnicityConflictException() -> CreateEventRepositoryException(
          request = request,
          cause = UnicityConflictException("An audience with this name already exists", it),
        )

        else -> CreateEventRepositoryException(request, it)
      }
    }

  override fun find(identifier: UUID): Either<GetEventException, Event> {
    return Either
      .catch {
        transaction(exposedDatabase.database) {
          EventTable
            .selectAll().where { EventTable.id eq identifier }
            .singleOrNull()
        }
      }
      .flatMap {
        it?.toEvent() ?: Either.Left(EventNotFoundException(identifier))
      }
      .mapLeft { GetEventException(identifier, it) }
  }

  private fun Throwable.isUnicityConflictException() =
    this is ExposedSQLException && this.sqlState == SQL_UNIQUE_VIOLATION_CODE
}
