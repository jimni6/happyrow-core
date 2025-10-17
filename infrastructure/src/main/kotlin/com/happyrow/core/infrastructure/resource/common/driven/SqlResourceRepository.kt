package com.happyrow.core.infrastructure.resource.common.driven

import arrow.core.Either
import arrow.core.flatMap
import com.happyrow.core.domain.resource.common.driven.ResourceRepository
import com.happyrow.core.domain.resource.common.error.CreateResourceRepositoryException
import com.happyrow.core.domain.resource.common.error.GetResourceRepositoryException
import com.happyrow.core.domain.resource.common.error.OptimisticLockException
import com.happyrow.core.domain.resource.common.error.ResourceNotFoundException
import com.happyrow.core.domain.resource.common.model.Resource
import com.happyrow.core.domain.resource.create.model.CreateResourceRequest
import com.happyrow.core.infrastructure.technical.config.ExposedDatabase
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Clock
import java.util.UUID

class SqlResourceRepository(
  private val clock: Clock,
  private val exposedDatabase: ExposedDatabase,
) : ResourceRepository {

  override fun create(request: CreateResourceRequest): Either<CreateResourceRepositoryException, Resource> =
    Either.catch {
      transaction(exposedDatabase.database) {
        ResourceTable.insertAndGetId {
          it[name] = request.name
          it[category] = request.category.name
          it[suggestedQuantity] = request.suggestedQuantity
          it[currentQuantity] = 0
          it[eventId] = request.eventId
          it[version] = 1
          it[createdAt] = clock.instant()
          it[updatedAt] = clock.instant()
        }.value
      }
    }
      .flatMap { resourceId ->
        find(resourceId)
          .mapLeft { CreateResourceRepositoryException(request, it) }
          .flatMap {
            it?.let { resource ->
              Either.Right(resource)
            } ?: Either.Left(
              CreateResourceRepositoryException(request, Exception("Resource not found after creation")),
            )
          }
      }
      .mapLeft { CreateResourceRepositoryException(request, it) }

  override fun find(resourceId: UUID): Either<GetResourceRepositoryException, Resource?> {
    return Either.catch {
      transaction(exposedDatabase.database) {
        ResourceTable
          .selectAll().where { ResourceTable.id eq resourceId }
          .singleOrNull()
      }
    }
      .flatMap { row ->
        row?.toResource() ?: Either.Right(null)
      }
      .mapLeft { GetResourceRepositoryException(null, it) }
  }

  override fun findByEvent(eventId: UUID): Either<GetResourceRepositoryException, List<Resource>> {
    return Either.catch {
      transaction(exposedDatabase.database) {
        ResourceTable
          .selectAll().where { ResourceTable.eventId eq eventId }
          .map { row ->
            row.toResource().fold(
              { error ->
                println("ERROR: Failed to parse resource row: ${row[ResourceTable.id].value} - ${error.message}")
                error.printStackTrace()
                throw error
              },
              { it },
            )
          }
      }
    }.mapLeft { GetResourceRepositoryException(eventId, it) }
  }

  override fun updateQuantity(
    resourceId: UUID,
    quantityDelta: Int,
    expectedVersion: Int,
  ): Either<GetResourceRepositoryException, Resource> {
    return Either.catch {
      transaction(exposedDatabase.database) {
        // First, get the current resource to read its quantity
        val currentResource = ResourceTable
          .selectAll().where {
            (ResourceTable.id eq resourceId) and (ResourceTable.version eq expectedVersion)
          }
          .singleOrNull()

        if (currentResource == null) {
          // Check if resource exists at all
          val existsWithDifferentVersion = ResourceTable
            .selectAll().where { ResourceTable.id eq resourceId }
            .singleOrNull()

          if (existsWithDifferentVersion == null) {
            throw ResourceNotFoundException(resourceId)
          } else {
            throw OptimisticLockException(resourceId, expectedVersion)
          }
        }

        val currentQuantity = currentResource[ResourceTable.currentQuantity]
        val newQuantity = currentQuantity + quantityDelta

        // Optimistic locking: update only if version still matches
        val updatedRows = ResourceTable.update({
          (ResourceTable.id eq resourceId) and (ResourceTable.version eq expectedVersion)
        }) {
          it[ResourceTable.currentQuantity] = newQuantity
          it[ResourceTable.version] = expectedVersion + 1
          it[ResourceTable.updatedAt] = clock.instant()
        }

        if (updatedRows == 0) {
          // Version changed between read and update
          throw OptimisticLockException(resourceId, expectedVersion)
        }

        resourceId
      }
    }
      .flatMap { id ->
        find(id)
          .flatMap {
            it?.let { resource ->
              Either.Right(resource)
            } ?: Either.Left(GetResourceRepositoryException(null, ResourceNotFoundException(id)))
          }
      }
      .mapLeft {
        when (it) {
          is OptimisticLockException -> GetResourceRepositoryException(null, it)
          is ResourceNotFoundException -> GetResourceRepositoryException(null, it)
          else -> GetResourceRepositoryException(null, it)
        }
      }
  }
}
