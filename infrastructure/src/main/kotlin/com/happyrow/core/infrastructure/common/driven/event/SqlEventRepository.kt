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
import com.happyrow.core.infrastructure.contribution.common.driven.ContributionTable
import com.happyrow.core.infrastructure.event.common.driven.event.EventTable
import com.happyrow.core.infrastructure.event.common.driven.event.toEvent
import com.happyrow.core.infrastructure.event.create.error.UnicityConflictException
import com.happyrow.core.infrastructure.event.delete.error.UnauthorizedDeleteException
import com.happyrow.core.infrastructure.participant.common.driven.ParticipantTable
import com.happyrow.core.infrastructure.resource.common.driven.ResourceTable
import com.happyrow.core.infrastructure.technical.config.ExposedDatabase
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import java.time.Clock
import java.util.UUID

private const val SQL_UNIQUE_VIOLATION_CODE = "23505"

class SqlEventRepository(
  private val clock: Clock,
  private val exposedDatabase: ExposedDatabase,
) : EventRepository {
  private val logger = LoggerFactory.getLogger(SqlEventRepository::class.java)
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

  override fun delete(identifier: UUID, userId: String): Either<DeleteEventRepositoryException, Unit> = Either
    .catch {
      transaction(exposedDatabase.database) {
        // First check if event exists and get creator
        val event = EventTable
          .selectAll().where { EventTable.id eq identifier }
          .singleOrNull()
          ?: throw EventNotFoundException(identifier)

        // Check if user is the creator
        val creatorId = event[EventTable.creator]
        if (creatorId != userId) {
          throw UnauthorizedDeleteException(identifier, userId)
        }

        // Cascade delete: First delete contributions (they reference participants and resources)
        val participantIds = ParticipantTable
          .selectAll().where { ParticipantTable.eventId eq identifier }
          .map { it[ParticipantTable.id].value }

        val resourceIds = ResourceTable
          .selectAll().where { ResourceTable.eventId eq identifier }
          .map { it[ResourceTable.id].value }

        // Delete contributions related to this event's participants
        if (participantIds.isNotEmpty()) {
          ContributionTable.deleteWhere {
            ContributionTable.participantId inList participantIds
          }
        }

        // Delete contributions related to this event's resources
        if (resourceIds.isNotEmpty()) {
          ContributionTable.deleteWhere {
            ContributionTable.resourceId inList resourceIds
          }
        }

        // Delete resources
        ResourceTable.deleteWhere { ResourceTable.eventId eq identifier }

        // Delete participants
        ParticipantTable.deleteWhere { ParticipantTable.eventId eq identifier }

        // Finally delete the event
        val deletedRows = EventTable.deleteWhere { EventTable.id eq identifier }
        if (deletedRows == 0) {
          throw EventNotFoundException(identifier)
        }
      }
    }
    .mapLeft {
      when (it) {
        is EventNotFoundException -> DeleteEventRepositoryException(identifier, it)
        is UnauthorizedDeleteException -> DeleteEventRepositoryException(identifier, it)
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
                  logger.error("Failed to parse event row: ${row[EventTable.id].value}", error)
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
