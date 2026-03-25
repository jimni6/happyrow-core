package com.happyrow.core.infrastructure.participant.common.driven

import arrow.core.Either
import arrow.core.flatMap
import com.happyrow.core.domain.participant.common.driven.ParticipantRepository
import com.happyrow.core.domain.participant.common.error.CreateParticipantRepositoryException
import com.happyrow.core.domain.participant.common.error.DeleteParticipantRepositoryException
import com.happyrow.core.domain.participant.common.error.GetParticipantRepositoryException
import com.happyrow.core.domain.participant.common.error.UpdateParticipantRepositoryException
import com.happyrow.core.domain.participant.common.model.Participant
import com.happyrow.core.domain.participant.common.model.ParticipantStatus
import com.happyrow.core.domain.participant.create.model.CreateParticipantRequest
import com.happyrow.core.domain.resource.common.error.OptimisticLockException
import com.happyrow.core.infrastructure.contribution.common.driven.ContributionTable
import com.happyrow.core.infrastructure.resource.common.driven.ResourceTable
import com.happyrow.core.infrastructure.technical.config.ExposedDatabase
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
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
          it[userName] = request.userName
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

  override fun delete(userEmail: String, eventId: UUID): Either<DeleteParticipantRepositoryException, Unit> =
    Either.catch {
      transaction(exposedDatabase.database) {
        val participant = ParticipantTable
          .selectAll().where { (ParticipantTable.userEmail eq userEmail) and (ParticipantTable.eventId eq eventId) }
          .singleOrNull() ?: throw NoSuchElementException("Participant $userEmail not found for event $eventId")

        val participantId = participant[ParticipantTable.id].value

        val contributions = ContributionTable
          .selectAll().where { ContributionTable.participantId eq participantId }
          .toList()

        for (contribution in contributions) {
          val resourceId = contribution[ContributionTable.resourceId]
          val contributedQty = contribution[ContributionTable.quantity]

          val resource = ResourceTable
            .selectAll().where { ResourceTable.id eq resourceId }
            .singleOrNull()

          if (resource != null) {
            val currentQty = resource[ResourceTable.currentQuantity]
            val currentVersion = resource[ResourceTable.version]
            val newQty = maxOf(0, currentQty - contributedQty)
            val updatedRows = ResourceTable.update({
              (ResourceTable.id eq resourceId) and (ResourceTable.version eq currentVersion)
            }) {
              it[currentQuantity] = newQty
              it[version] = currentVersion + 1
              it[updatedAt] = clock.instant()
            }
            if (updatedRows == 0) {
              throw OptimisticLockException(resourceId, currentVersion)
            }
          }
        }

        ContributionTable.deleteWhere { ContributionTable.participantId eq participantId }

        ParticipantTable.deleteWhere {
          (ParticipantTable.userEmail eq userEmail) and (ParticipantTable.eventId eq eventId)
        }
        Unit
      }
    }.mapLeft { DeleteParticipantRepositoryException(userEmail, eventId, it) }

  override fun findOrCreate(
    userEmail: String,
    eventId: UUID,
  ): Either<CreateParticipantRepositoryException, Participant> {
    return find(userEmail, eventId)
      .mapLeft {
        CreateParticipantRepositoryException(CreateParticipantRequest(userEmail = userEmail, eventId = eventId), it)
      }
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
