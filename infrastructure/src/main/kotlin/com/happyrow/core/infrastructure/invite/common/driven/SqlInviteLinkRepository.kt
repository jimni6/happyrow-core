package com.happyrow.core.infrastructure.invite.common.driven

import arrow.core.Either
import com.happyrow.core.domain.invite.common.driven.InviteLinkRepository
import com.happyrow.core.domain.invite.common.error.InviteLinkRepositoryException
import com.happyrow.core.domain.invite.common.model.AcceptInviteResult
import com.happyrow.core.domain.invite.common.model.InviteLink
import com.happyrow.core.domain.invite.common.model.InviteStatus
import com.happyrow.core.domain.invite.create.model.CreateInviteLinkRequest
import com.happyrow.core.infrastructure.participant.common.driven.ParticipantTable
import com.happyrow.core.infrastructure.technical.config.ExposedDatabase
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.security.SecureRandom
import java.time.Clock
import java.util.UUID

private const val TOKEN_LENGTH = 32

class SqlInviteLinkRepository(
  private val clock: Clock,
  private val exposedDatabase: ExposedDatabase,
) : InviteLinkRepository {

  override fun create(request: CreateInviteLinkRequest): Either<InviteLinkRepositoryException, InviteLink> =
    Either.catch {
      transaction(exposedDatabase.database) {
        val now = clock.instant()
        val token = generateToken()
        val expiresAt = now.plusSeconds(request.expiresInDays.toLong() * 86400)

        val id = EventInviteTable.insertAndGetId {
          it[EventInviteTable.token] = token
          it[eventId] = request.eventId
          it[createdBy] = request.createdBy
          it[status] = InviteStatus.ACTIVE.name
          it[maxUses] = request.maxUses
          it[currentUses] = 0
          it[createdAt] = now
          it[EventInviteTable.expiresAt] = expiresAt
        }.value

        EventInviteTable.selectAll()
          .where { EventInviteTable.id eq id }
          .single()
          .toInviteLink()
      }
    }.mapLeft { InviteLinkRepositoryException("Failed to create invite link", it) }

  override fun findByToken(token: String): Either<InviteLinkRepositoryException, InviteLink?> = Either.catch {
    transaction(exposedDatabase.database) {
      EventInviteTable.selectAll()
        .where { EventInviteTable.token eq token }
        .singleOrNull()
        ?.toInviteLink()
    }
  }.mapLeft { InviteLinkRepositoryException("Failed to find invite by token", it) }

  override fun findActiveByEventId(eventId: UUID): Either<InviteLinkRepositoryException, InviteLink?> = Either.catch {
    transaction(exposedDatabase.database) {
      val now = clock.instant()
      val invite = EventInviteTable.selectAll()
        .where {
          (EventInviteTable.eventId eq eventId) and
            (EventInviteTable.status eq InviteStatus.ACTIVE.name)
        }
        .singleOrNull()
        ?.toInviteLink()

      if (invite != null && invite.expiresAt.isBefore(now)) {
        EventInviteTable.update({ EventInviteTable.token eq invite.token }) {
          it[status] = InviteStatus.EXPIRED.name
        }
        null
      } else if (invite != null && invite.maxUses?.let { invite.currentUses >= it } == true) {
        null
      } else {
        invite
      }
    }
  }.mapLeft { InviteLinkRepositoryException("Failed to find active invite", it) }

  override fun updateStatus(token: String, status: InviteStatus): Either<InviteLinkRepositoryException, Unit> =
    Either.catch {
      transaction(exposedDatabase.database) {
        EventInviteTable.update({ EventInviteTable.token eq token }) {
          it[EventInviteTable.status] = status.name
        }
      }
      Unit
    }.mapLeft { InviteLinkRepositoryException("Failed to update invite status", it) }

  override fun acceptInvite(
    token: String,
    userId: UUID,
    userName: String?,
    eventId: UUID,
  ): Either<InviteLinkRepositoryException, AcceptInviteResult> = Either.catch {
    transaction(exposedDatabase.database) {
      val now = clock.instant()

      ParticipantTable.insertAndGetId {
        it[ParticipantTable.userId] = userId
        it[ParticipantTable.userName] = userName
        it[ParticipantTable.eventId] = eventId
        it[ParticipantTable.status] = "CONFIRMED"
        it[joinedAt] = now
        it[createdAt] = now
        it[updatedAt] = now
      }

      EventInviteTable.update({ EventInviteTable.token eq token }) {
        with(org.jetbrains.exposed.sql.SqlExpressionBuilder) {
          it[currentUses] = currentUses + 1
        }
      }

      AcceptInviteResult(
        eventId = eventId,
        userId = userId,
        userName = userName,
        status = "CONFIRMED",
        joinedAt = now,
      )
    }
  }.mapLeft { InviteLinkRepositoryException("Failed to accept invite", it) }

  override fun countConfirmedParticipants(eventId: UUID): Either<InviteLinkRepositoryException, Long> = Either.catch {
    transaction(exposedDatabase.database) {
      ParticipantTable.selectAll()
        .where {
          (ParticipantTable.eventId eq eventId) and
            (ParticipantTable.status eq "CONFIRMED")
        }
        .count()
    }
  }.mapLeft { InviteLinkRepositoryException("Failed to count participants", it) }

  private fun generateToken(): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    val random = SecureRandom()
    return (1..TOKEN_LENGTH).map { chars[random.nextInt(chars.length)] }.joinToString("")
  }
}
