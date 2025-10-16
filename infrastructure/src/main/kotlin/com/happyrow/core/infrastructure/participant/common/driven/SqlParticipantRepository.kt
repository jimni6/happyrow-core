package com.happyrow.core.infrastructure.participant.common.driven

import arrow.core.Either
import arrow.core.flatMap
import com.happyrow.core.domain.participant.common.driven.ParticipantRepository
import com.happyrow.core.domain.participant.common.error.CreateParticipantRepositoryException
import com.happyrow.core.domain.participant.common.error.GetParticipantRepositoryException
import com.happyrow.core.domain.participant.common.model.Participant
import com.happyrow.core.domain.participant.common.model.ParticipantStatus
import com.happyrow.core.domain.participant.create.model.CreateParticipantRequest
import com.happyrow.core.infrastructure.technical.config.ExposedDatabase
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Clock
import java.util.UUID

class SqlParticipantRepository(
  private val clock: Clock,
  private val exposedDatabase: ExposedDatabase,
) : ParticipantRepository {

  override fun create(request: CreateParticipantRequest): Either<CreateParticipantRepositoryException, Participant> =
    Either.catch {
      transaction(exposedDatabase.database) {
        ParticipantTable.insertAndGetId {
          it[userId] = request.userId
          it[eventId] = request.eventId
          it[status] = request.status.name
          it[joinedAt] = clock.instant()
          it[createdAt] = clock.instant()
          it[updatedAt] = clock.instant()
        }.value
      }
    }
      .flatMap { participantId ->
        find(request.userId, request.eventId)
          .mapLeft { CreateParticipantRepositoryException(request, it) }
          .flatMap {
            it?.let {
              Either.Right(it)
            } ?: Either.Left(
              CreateParticipantRepositoryException(request, Exception("Participant not found after creation")),
            )
          }
      }
      .mapLeft { CreateParticipantRepositoryException(request, it) }

  override fun findOrCreate(userId: UUID, eventId: UUID): Either<CreateParticipantRepositoryException, Participant> {
    return find(userId, eventId)
      .mapLeft { CreateParticipantRepositoryException(CreateParticipantRequest(userId, eventId), it) }
      .flatMap { existing ->
        if (existing != null) {
          Either.Right(existing)
        } else {
          create(CreateParticipantRequest(userId = userId, eventId = eventId, status = ParticipantStatus.CONFIRMED))
        }
      }
  }

  override fun findByEvent(eventId: UUID): Either<GetParticipantRepositoryException, List<Participant>> {
    return Either.catch {
      transaction(exposedDatabase.database) {
        ParticipantTable
          .selectAll().where { ParticipantTable.eventId eq eventId }
          .map { row ->
            row.toParticipant().fold(
              { error ->
                println("ERROR: Failed to parse participant row: ${row[ParticipantTable.id].value} - ${error.message}")
                error.printStackTrace()
                throw error
              },
              { it },
            )
          }
      }
    }.mapLeft { GetParticipantRepositoryException(eventId, it) }
  }

  override fun find(userId: UUID, eventId: UUID): Either<GetParticipantRepositoryException, Participant?> {
    return Either.catch {
      transaction(exposedDatabase.database) {
        ParticipantTable
          .selectAll().where { (ParticipantTable.userId eq userId) and (ParticipantTable.eventId eq eventId) }
          .singleOrNull()
      }
    }
      .flatMap { row ->
        row?.toParticipant() ?: Either.Right(null)
      }
      .mapLeft { GetParticipantRepositoryException(eventId, it) }
  }
}
