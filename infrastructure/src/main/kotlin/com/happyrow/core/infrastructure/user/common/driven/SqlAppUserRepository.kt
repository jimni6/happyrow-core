package com.happyrow.core.infrastructure.user.common.driven

import arrow.core.Either
import com.happyrow.core.domain.user.common.driven.AppUserRepository
import com.happyrow.core.domain.user.common.model.AppUser
import com.happyrow.core.infrastructure.technical.config.ExposedDatabase
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Clock
import java.util.UUID

class SqlAppUserRepository(
  private val clock: Clock,
  private val exposedDatabase: ExposedDatabase,
) : AppUserRepository {

  override fun findOrCreate(id: UUID, email: String): Either<Exception, AppUser> = Either.catch {
    transaction(exposedDatabase.database) {
      val inserted = AppUserTable.insertIgnore {
        it[AppUserTable.id] = id
        it[AppUserTable.email] = email
        it[createdAt] = clock.instant()
        it[updatedAt] = clock.instant()
      }.insertedCount > 0

      if (!inserted) {
        AppUserTable.update({ AppUserTable.id eq id }) {
          it[AppUserTable.email] = email
          it[updatedAt] = clock.instant()
        }
      }

      AppUserTable.selectAll().where { AppUserTable.id eq id }
        .single()
        .let { row ->
          AppUser(
            id = row[AppUserTable.id].value,
            email = row[AppUserTable.email],
            name = row[AppUserTable.name],
            createdAt = row[AppUserTable.createdAt],
            updatedAt = row[AppUserTable.updatedAt],
          )
        }
    }
  }.mapLeft { Exception("Failed to find or create user $id", it) }

  override fun findById(id: UUID): Either<Exception, AppUser?> = Either.catch {
    transaction(exposedDatabase.database) {
      AppUserTable.selectAll().where { AppUserTable.id eq id }
        .singleOrNull()
        ?.let { row ->
          AppUser(
            id = row[AppUserTable.id].value,
            email = row[AppUserTable.email],
            name = row[AppUserTable.name],
            createdAt = row[AppUserTable.createdAt],
            updatedAt = row[AppUserTable.updatedAt],
          )
        }
    }
  }.mapLeft { Exception("Failed to find user $id", it) }
}
