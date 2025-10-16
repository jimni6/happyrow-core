package com.happyrow.core.infrastructure.contribution.common.driven

import arrow.core.Either
import arrow.core.flatMap
import com.happyrow.core.domain.contribution.add.model.AddContributionRequest
import com.happyrow.core.domain.contribution.common.driven.ContributionRepository
import com.happyrow.core.domain.contribution.common.error.ContributionRepositoryException
import com.happyrow.core.domain.contribution.common.model.Contribution
import com.happyrow.core.domain.participant.common.driven.ParticipantRepository
import com.happyrow.core.domain.resource.common.driven.ResourceRepository
import com.happyrow.core.infrastructure.technical.config.ExposedDatabase
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Clock
import java.util.UUID

class SqlContributionRepository(
  private val clock: Clock,
  private val exposedDatabase: ExposedDatabase,
  private val participantRepository: ParticipantRepository,
  private val resourceRepository: ResourceRepository,
) : ContributionRepository {

  override fun addOrUpdate(request: AddContributionRequest): Either<ContributionRepositoryException, Contribution> {
    return participantRepository.findOrCreate(request.userId, request.eventId)
      .mapLeft { ContributionRepositoryException(request.resourceId, it) }
      .flatMap { participant ->
        // Check if contribution already exists
        val existingContribution = Either.catch {
          transaction(exposedDatabase.database) {
            ContributionTable
              .selectAll().where {
                (ContributionTable.participantId eq participant.identifier) and
                  (ContributionTable.resourceId eq request.resourceId)
              }
              .singleOrNull()
          }
        }.mapLeft { ContributionRepositoryException(request.resourceId, it) }

        existingContribution.flatMap { existing ->
          if (existing != null) {
            // Update existing contribution
            val oldQuantity = existing[ContributionTable.quantity]
            val quantityDelta = request.quantity - oldQuantity

            updateContribution(participant.identifier, request.resourceId, request.quantity)
              .flatMap { contribution ->
                // Update resource quantity
                resourceRepository.find(request.resourceId)
                  .mapLeft { ContributionRepositoryException(request.resourceId, it) }
                  .flatMap { resource ->
                    resource?.let {
                      resourceRepository.updateQuantity(request.resourceId, quantityDelta, it.version)
                        .mapLeft { ContributionRepositoryException(request.resourceId, it) }
                        .map { contribution }
                    } ?: Either.Left(
                      ContributionRepositoryException(
                        request.resourceId,
                        Exception("Resource not found"),
                      ),
                    )
                  }
              }
          } else {
            // Create new contribution
            createContribution(participant.identifier, request.resourceId, request.quantity)
              .flatMap { contribution ->
                // Update resource quantity
                resourceRepository.find(request.resourceId)
                  .mapLeft { ContributionRepositoryException(request.resourceId, it) }
                  .flatMap { resource ->
                    resource?.let {
                      resourceRepository.updateQuantity(request.resourceId, request.quantity, it.version)
                        .mapLeft { ContributionRepositoryException(request.resourceId, it) }
                        .map { contribution }
                    } ?: Either.Left(
                      ContributionRepositoryException(
                        request.resourceId,
                        Exception("Resource not found"),
                      ),
                    )
                  }
              }
          }
        }
      }
  }

  override fun delete(userId: UUID, eventId: UUID, resourceId: UUID): Either<ContributionRepositoryException, Unit> {
    return participantRepository.find(userId, eventId)
      .mapLeft { ContributionRepositoryException(resourceId, it) }
      .flatMap { participant ->
        participant?.let {
          // Get contribution quantity before deletion
          val contributionQuantity = Either.catch {
            transaction(exposedDatabase.database) {
              ContributionTable
                .selectAll().where {
                  (ContributionTable.participantId eq participant.identifier) and
                    (ContributionTable.resourceId eq resourceId)
                }
                .singleOrNull()
                ?.get(ContributionTable.quantity)
            }
          }.mapLeft { ContributionRepositoryException(resourceId, it) }

          contributionQuantity.flatMap { quantity ->
            quantity?.let {
              // Delete contribution
              Either.catch {
                transaction(exposedDatabase.database) {
                  ContributionTable.deleteWhere {
                    (ContributionTable.participantId eq participant.identifier) and
                      (ContributionTable.resourceId eq resourceId)
                  }
                }
              }
                .mapLeft { ContributionRepositoryException(resourceId, it) }
                .flatMap {
                  // Update resource quantity (decrease by contribution amount)
                  resourceRepository.find(resourceId)
                    .mapLeft { ContributionRepositoryException(resourceId, it) }
                    .flatMap { resource ->
                      resource?.let { res ->
                        resourceRepository.updateQuantity(resourceId, -quantity, res.version)
                          .mapLeft { ContributionRepositoryException(resourceId, it) }
                          .map { }
                      } ?: Either.Right(Unit)
                    }
                }
            } ?: Either.Right(Unit) // Contribution doesn't exist, nothing to delete
          }
        } ?: Either.Right(Unit) // Participant doesn't exist, nothing to delete
      }
  }

  override fun findByResource(resourceId: UUID): Either<ContributionRepositoryException, List<Contribution>> {
    return Either.catch {
      transaction(exposedDatabase.database) {
        ContributionTable
          .selectAll().where { ContributionTable.resourceId eq resourceId }
          .map { row ->
            row.toContribution().fold(
              { error ->
                println("ERROR: Failed to parse contribution row: ${row[ContributionTable.id].value}")
                error.printStackTrace()
                throw error
              },
              { it },
            )
          }
      }
    }.mapLeft { ContributionRepositoryException(resourceId, it) }
  }

  private fun createContribution(
    participantId: UUID,
    resourceId: UUID,
    quantity: Int,
  ): Either<ContributionRepositoryException, Contribution> {
    return Either.catch {
      transaction(exposedDatabase.database) {
        val contributionId = ContributionTable.insert {
          it[ContributionTable.participantId] = participantId
          it[ContributionTable.resourceId] = resourceId
          it[ContributionTable.quantity] = quantity
          it[createdAt] = clock.instant()
          it[updatedAt] = clock.instant()
        }[ContributionTable.id].value

        ContributionTable
          .selectAll().where { ContributionTable.id eq contributionId }
          .single()
          .toContribution()
          .getOrNull()!!
      }
    }.mapLeft { ContributionRepositoryException(resourceId, it) }
  }

  private fun updateContribution(
    participantId: UUID,
    resourceId: UUID,
    quantity: Int,
  ): Either<ContributionRepositoryException, Contribution> {
    return Either.catch {
      transaction(exposedDatabase.database) {
        ContributionTable.update({
          (ContributionTable.participantId eq participantId) and
            (ContributionTable.resourceId eq resourceId)
        }) {
          it[ContributionTable.quantity] = quantity
          it[updatedAt] = clock.instant()
        }

        ContributionTable
          .selectAll().where {
            (ContributionTable.participantId eq participantId) and
              (ContributionTable.resourceId eq resourceId)
          }
          .single()
          .toContribution()
          .getOrNull()!!
      }
    }.mapLeft { ContributionRepositoryException(resourceId, it) }
  }
}
