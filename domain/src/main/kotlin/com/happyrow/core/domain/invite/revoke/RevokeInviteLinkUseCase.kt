package com.happyrow.core.domain.invite.revoke

import arrow.core.Either
import arrow.core.flatMap
import com.happyrow.core.domain.event.common.driven.event.EventRepository
import com.happyrow.core.domain.invite.common.driven.InviteLinkRepository
import com.happyrow.core.domain.invite.common.model.InviteStatus
import com.happyrow.core.domain.invite.revoke.error.RevokeInviteLinkException
import java.util.UUID

class RevokeInviteLinkUseCase(
  private val inviteLinkRepository: InviteLinkRepository,
  private val eventRepository: EventRepository,
) {
  fun execute(eventId: UUID, token: String, userId: String): Either<RevokeInviteLinkException, Unit> =
    eventRepository.find(eventId)
      .mapLeft { RevokeInviteLinkException("Event not found: $eventId", it) }
      .flatMap { event ->
        if (event.creator.value.toString() != userId) {
          Either.Left(RevokeInviteLinkException("FORBIDDEN"))
        } else {
          Either.Right(Unit)
        }
      }
      .flatMap {
        inviteLinkRepository.findByToken(token)
          .mapLeft { RevokeInviteLinkException("Failed to find invite", it) }
      }
      .flatMap { invite ->
        when {
          invite == null -> Either.Left(RevokeInviteLinkException("NOT_FOUND"))
          invite.eventId != eventId -> Either.Left(RevokeInviteLinkException("NOT_FOUND"))
          invite.status == InviteStatus.REVOKED -> Either.Left(RevokeInviteLinkException("ALREADY_REVOKED"))
          else -> Either.Right(Unit)
        }
      }
      .flatMap {
        inviteLinkRepository.updateStatus(token, InviteStatus.REVOKED)
          .mapLeft { RevokeInviteLinkException("Failed to revoke invite", it) }
      }
}
