package com.happyrow.core.infrastructure.common.driven.event

import arrow.core.Either
import arrow.core.flatMap
import com.happyrow.core.domain.event.common.driven.event.EventRepository
import com.happyrow.core.domain.event.common.error.CreateEventRepositoryException
import com.happyrow.core.domain.event.common.error.DeleteEventRepositoryException
import com.happyrow.core.domain.event.common.error.EventNotFoundException
import com.happyrow.core.domain.event.common.error.UpdateEventRepositoryException
import com.happyrow.core.domain.event.common.model.event.Event
import com.happyrow.core.domain.event.create.model.CreateEventRequest
import com.happyrow.core.domain.event.creator.model.Creator
import com.happyrow.core.domain.event.get.error.GetEventException
import com.happyrow.core.domain.event.update.model.UpdateEventRequest
import com.happyrow.core.infrastructure.event.common.driven.event.EventTable
import com.happyrow.core.infrastructure.event.common.driven.event.toEvent
import com.happyrow.core.infrastructure.event.create.error.UnicityConflictException
import com.happyrow.core.infrastructure.technical.config.ExposedDatabase
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Clock
import java.util.UUID

private const val SQL_UNIQUE_VIOLATION_CODE = "23505"

class SqlEventRepository(
  private val clock: Clock,
  private val exposedDatabase: ExposedDatabase,
) : EventRepository {
  override fun create(request: CreateEventRequest): Either<CreateEventRepositoryException, Event> = Either
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

  override fun update(request: UpdateEventRequest): Either<UpdateEventRepositoryException, Event> = Either
    .catch {
      transaction(exposedDatabase.database) {
        val updatedRows = EventTable.update({ EventTable.id eq request.identifier }) {
          it[name] = request.name
          it[description] = request.description
          it[eventDate] = request.eventDate
          it[location] = request.location
          it[type] = request.type
          it[updateDate] = clock.instant()
          it[members] = request.members.map { member -> UUID.fromString(member.toString()) }
        }
        if (updatedRows == 0) {
          throw EventNotFoundException(request.identifier)
        }
        request.identifier
      }
    }
    .flatMap { find(it) }
    .mapLeft {
      when {
        it.isUnicityConflictException() -> UpdateEventRepositoryException(
          request = request,
          cause = UnicityConflictException("An event with this name already exists", it),
        )
        it is EventNotFoundException -> UpdateEventRepositoryException(request, it)
        else -> UpdateEventRepositoryException(request, it)
      }
    }

  override fun delete(identifier: UUID): Either<DeleteEventRepositoryException, Unit> = Either
    .catch {
      transaction(exposedDatabase.database) {
        val deletedRows = EventTable.deleteWhere { EventTable.id eq identifier }
        if (deletedRows == 0) {
          throw EventNotFoundException(identifier)
        }
      }
    }
    .mapLeft {
      when (it) {
        is EventNotFoundException -> DeleteEventRepositoryException(identifier, it)
        else -> DeleteEventRepositoryException(identifier, it)
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

  override fun findByOrganizer(organizer: Creator): Either<GetEventException, List<Event>> {
    return Either
      .catch {
        transaction(exposedDatabase.database) {
          EventTable
            .selectAll().where { EventTable.creator eq organizer.toString() }
            .map { row ->
              row.toEvent().fold(
                { error ->
                  println("ERROR: Failed to parse event row: ${row[EventTable.id].value} - ${error.message}")
                  error.cause?.printStackTrace()
                  throw error
                },
                { it },
              )
            }
        }
      }
      .mapLeft { GetEventException(null, it) }
  }

  private fun Throwable.isUnicityConflictException() =
    this is ExposedSQLException && this.sqlState == SQL_UNIQUE_VIOLATION_CODE
}
