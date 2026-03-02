package com.happyrow.core.infrastructure.participant.common.driven

import arrow.core.Either
import arrow.core.flatMap
import com.happyrow.core.domain.participant.common.driven.ParticipantRepository
import com.happyrow.core.domain.participant.common.error.CreateParticipantRepositoryException
import com.happyrow.core.domain.participant.common.error.GetParticipantRepositoryException
import com.happyrow.core.domain.participant.common.error.UpdateParticipantRepositoryException
import com.happyrow.core.domain.participant.common.model.Participant
import com.happyrow.core.domain.participant.common.model.ParticipantStatus
import com.happyrow.core.domain.participant.create.model.CreateParticipantRequest
import com.happyrow.core.infrastructure.technical.config.ExposedDatabase
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import java.time.Clock
import java.util.UUID

class SqlParticipantRepository(
  private val clock: Clock,
  private val exposedDatabase: ExposedDatabase,
) : ParticipantRepository {
  private val logger = LoggerFactory.getLogger(SqlParticipantRepository::class.java)

  override fun create(request: CreateParticipantRequest): Either<CreateParticipantRepositoryException, Participant> =
    Either.catch {
      transaction(exposedDatabase.database) {
        ParticipantTable.insertAndGetId {
          it[userEmail] = request.userEmail
          it[eventId] = request.eventId
          it[status] = request.status.name
          it[joinedAt] = clock.instant()
          it[createdAt] = clock.instant()
          it[updatedAt] = clock.instant()
        }.value
      }
    }
      .flatMap { participantId ->
        find(request.userEmail, request.eventId)
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

  override fun update(participant: Participant): Either<UpdateParticipantRepositoryException, Participant> =
    Either.catch {
      transaction(exposedDatabase.database) {
        ParticipantTable.update({
          (ParticipantTable.userEmail eq participant.userEmail) and (ParticipantTable.eventId eq participant.eventId)
        }) {
          it[status] = participant.status.name
          it[updatedAt] = clock.instant()
        }
      }
    }
      .flatMap {
        find(participant.userEmail, participant.eventId)
          .mapLeft { UpdateParticipantRepositoryException(participant.userEmail, participant.eventId, it) }
          .flatMap {
            it?.let {
              Either.Right(it)
            } ?: Either.Left(
              UpdateParticipantRepositoryException(
                participant.userEmail,
                participant.eventId,
                Exception("Participant not found after update"),
              ),
            )
          }
      }
      .mapLeft { UpdateParticipantRepositoryException(participant.userEmail, participant.eventId, it) }

  override fun findOrCreate(
    userEmail: String,
    eventId: UUID,
  ): Either<CreateParticipantRepositoryException, Participant> {
    return find(userEmail, eventId)
      .mapLeft { CreateParticipantRepositoryException(CreateParticipantRequest(userEmail, eventId), it) }
      .flatMap { existing ->
        if (existing != null) {
          Either.Right(existing)
        } else {
          create(
            CreateParticipantRequest(userEmail = userEmail, eventId = eventId, status = ParticipantStatus.CONFIRMED),
          )
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
                logger.error("Failed to parse participant row: ${row[ParticipantTable.id].value}", error)
                throw error
              },
              { it },
            )
          }
      }
    }.mapLeft { GetParticipantRepositoryException(eventId, it) }
  }

  override fun find(userEmail: String, eventId: UUID): Either<GetParticipantRepositoryException, Participant?> {
    return Either.catch {
      transaction(exposedDatabase.database) {
        ParticipantTable
          .selectAll().where { (ParticipantTable.userEmail eq userEmail) and (ParticipantTable.eventId eq eventId) }
          .singleOrNull()
      }
    }
      .flatMap { row ->
        row?.toParticipant() ?: Either.Right(null)
      }
      .mapLeft { GetParticipantRepositoryException(eventId, it) }
  }

  override fun findById(participantId: UUID): Either<GetParticipantRepositoryException, Participant?> {
    return Either.catch {
      transaction(exposedDatabase.database) {
        ParticipantTable
          .selectAll().where { ParticipantTable.id eq participantId }
          .singleOrNull()
      }
    }
      .flatMap { row ->
        row?.toParticipant() ?: Either.Right(null)
      }
      .mapLeft { GetParticipantRepositoryException(participantId, it) }
  }
}
