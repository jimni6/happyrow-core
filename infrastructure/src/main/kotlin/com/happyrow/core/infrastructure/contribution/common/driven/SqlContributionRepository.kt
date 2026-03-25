package com.happyrow.core.infrastructure.contribution.common.driven

import arrow.core.Either
import com.happyrow.core.domain.contribution.add.model.AddContributionRequest
import com.happyrow.core.domain.contribution.common.driven.ContributionRepository
import com.happyrow.core.domain.contribution.common.error.ContributionRepositoryException
import com.happyrow.core.domain.contribution.common.model.Contribution
import com.happyrow.core.domain.contribution.reduce.error.InsufficientContributionException
import com.happyrow.core.domain.contribution.reduce.model.ReduceContributionRequest
import com.happyrow.core.domain.participant.common.model.ParticipantStatus
import com.happyrow.core.domain.resource.common.error.OptimisticLockException
import com.happyrow.core.domain.resource.common.error.ResourceNotFoundException
import com.happyrow.core.infrastructure.participant.common.driven.ParticipantTable
import com.happyrow.core.infrastructure.resource.common.driven.ResourceTable
import com.happyrow.core.infrastructure.technical.config.ExposedDatabase
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import java.time.Clock
import java.util.UUID

class SqlContributionRepository(
  private val clock: Clock,
  private val exposedDatabase: ExposedDatabase,
) : ContributionRepository {
  private val logger = LoggerFactory.getLogger(SqlContributionRepository::class.java)

  override fun addOrUpdate(request: AddContributionRequest): Either<ContributionRepositoryException, Contribution> {
    return Either.catch {
      transaction(exposedDatabase.database) {
        val participantId = findOrCreateParticipantInTx(request.userId, request.eventId)

        val existing = ContributionTable
          .selectAll().where {
            (ContributionTable.participantId eq participantId) and
              (ContributionTable.resourceId eq request.resourceId)
          }
          .singleOrNull()

        val delta: Int
        if (existing != null) {
          val oldQuantity = existing[ContributionTable.quantity]
          delta = request.quantity - oldQuantity
          ContributionTable.update({
            (ContributionTable.participantId eq participantId) and
              (ContributionTable.resourceId eq request.resourceId)
          }) {
            it[quantity] = request.quantity
            it[updatedAt] = clock.instant()
          }
        } else {
          delta = request.quantity
          ContributionTable.insert {
            it[ContributionTable.participantId] = participantId
            it[ContributionTable.resourceId] = request.resourceId
            it[quantity] = request.quantity
            it[createdAt] = clock.instant()
            it[updatedAt] = clock.instant()
          }
        }

        updateResourceQuantityInTx(request.resourceId, delta)

        ContributionTable
          .selectAll().where {
            (ContributionTable.participantId eq participantId) and
              (ContributionTable.resourceId eq request.resourceId)
          }
          .single()
          .toContribution()
          .getOrNull()!!
      }
    }.mapLeft { ContributionRepositoryException(request.resourceId, it) }
  }

  override fun reduce(request: ReduceContributionRequest): Either<ContributionRepositoryException, Contribution?> {
    return Either.catch {
      transaction(exposedDatabase.database) {
        val participant = ParticipantTable
          .selectAll().where {
            (ParticipantTable.userId eq request.userId) and
              (ParticipantTable.eventId eq request.eventId)
          }
          .singleOrNull() ?: throw NoSuchElementException(
          "Participant ${request.userId} not found for event ${request.eventId}",
        )

        val participantId = participant[ParticipantTable.id].value
        val contribution = ContributionTable
          .selectAll().where {
            (ContributionTable.participantId eq participantId) and
              (ContributionTable.resourceId eq request.resourceId)
          }
          .singleOrNull() ?: throw NoSuchElementException(
          "Contribution not found for participant $participantId and resource ${request.resourceId}",
        )

        val currentQuantity = contribution[ContributionTable.quantity]
        if (request.quantity > currentQuantity) {
          throw InsufficientContributionException(currentQuantity, request.quantity)
        }

        val newQuantity = currentQuantity - request.quantity
        if (newQuantity == 0) {
          ContributionTable.deleteWhere {
            (ContributionTable.participantId eq participantId) and
              (ContributionTable.resourceId eq request.resourceId)
          }
          updateResourceQuantityInTx(request.resourceId, -request.quantity)
          null
        } else {
          ContributionTable.update({
            (ContributionTable.participantId eq participantId) and
              (ContributionTable.resourceId eq request.resourceId)
          }) {
            it[quantity] = newQuantity
            it[updatedAt] = clock.instant()
          }
          updateResourceQuantityInTx(request.resourceId, -request.quantity)

          ContributionTable
            .selectAll().where {
              (ContributionTable.participantId eq participantId) and
                (ContributionTable.resourceId eq request.resourceId)
            }
            .single()
            .toContribution()
            .getOrNull()!!
        }
      }
    }.mapLeft { ContributionRepositoryException(request.resourceId, it) }
  }

  override fun delete(userId: UUID, eventId: UUID, resourceId: UUID): Either<ContributionRepositoryException, Unit> {
    return Either.catch {
      transaction(exposedDatabase.database) {
        val participant = ParticipantTable
          .selectAll().where {
            (ParticipantTable.userId eq userId) and (ParticipantTable.eventId eq eventId)
          }
          .singleOrNull() ?: return@transaction

        val participantId = participant[ParticipantTable.id].value
        val contribution = ContributionTable
          .selectAll().where {
            (ContributionTable.participantId eq participantId) and
              (ContributionTable.resourceId eq resourceId)
          }
          .singleOrNull() ?: return@transaction

        val quantity = contribution[ContributionTable.quantity]
        ContributionTable.deleteWhere {
          (ContributionTable.participantId eq participantId) and
            (ContributionTable.resourceId eq resourceId)
        }
        updateResourceQuantityInTx(resourceId, -quantity)
      }
    }.mapLeft { ContributionRepositoryException(resourceId, it) }
  }

  override fun findByResource(resourceId: UUID): Either<ContributionRepositoryException, List<Contribution>> {
    return Either.catch {
      transaction(exposedDatabase.database) {
        ContributionTable
          .selectAll().where { ContributionTable.resourceId eq resourceId }
          .map { row ->
            row.toContribution().fold(
              { error ->
                logger.error("Failed to parse contribution row: ${row[ContributionTable.id].value}", error)
                throw error
              },
              { it },
            )
          }
      }
    }.mapLeft { ContributionRepositoryException(resourceId, it) }
  }

  private fun findOrCreateParticipantInTx(userId: UUID, eventId: UUID): UUID {
    val existing = ParticipantTable
      .selectAll().where { (ParticipantTable.userId eq userId) and (ParticipantTable.eventId eq eventId) }
      .singleOrNull()

    return existing?.get(ParticipantTable.id)?.value
      ?: ParticipantTable.insertAndGetId {
        it[ParticipantTable.userId] = userId
        it[ParticipantTable.eventId] = eventId
        it[ParticipantTable.status] = ParticipantStatus.CONFIRMED.name
        it[ParticipantTable.joinedAt] = clock.instant()
        it[ParticipantTable.createdAt] = clock.instant()
        it[ParticipantTable.updatedAt] = clock.instant()
      }.value
  }

  private fun updateResourceQuantityInTx(resourceId: UUID, quantityDelta: Int) {
    val resource = ResourceTable
      .selectAll().where { ResourceTable.id eq resourceId }
      .singleOrNull() ?: throw ResourceNotFoundException(resourceId)

    val currentVersion = resource[ResourceTable.version]
    val newQuantity = resource[ResourceTable.currentQuantity] + quantityDelta
    require(newQuantity >= 0) { "Resource quantity cannot become negative" }

    val updatedRows = ResourceTable.update({
      (ResourceTable.id eq resourceId) and (ResourceTable.version eq currentVersion)
    }) {
      it[ResourceTable.currentQuantity] = newQuantity
      it[ResourceTable.version] = currentVersion + 1
      it[ResourceTable.updatedAt] = clock.instant()
    }
    if (updatedRows == 0) throw OptimisticLockException(resourceId, currentVersion)
  }
}
