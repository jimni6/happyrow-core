package com.happyrow.core.domain.invite.accept

import arrow.core.Either
import arrow.core.flatMap
import com.happyrow.core.domain.invite.accept.error.AcceptInviteException
import com.happyrow.core.domain.invite.common.driven.InviteLinkRepository
import com.happyrow.core.domain.invite.common.model.AcceptInviteResult
import com.happyrow.core.domain.invite.common.model.InviteLink
import com.happyrow.core.domain.invite.common.model.InviteStatus
import com.happyrow.core.domain.participant.common.driven.ParticipantRepository
import java.time.Clock
import java.util.UUID

class AcceptInviteUseCase(
  private val inviteLinkRepository: InviteLinkRepository,
  private val participantRepository: ParticipantRepository,
  private val clock: Clock,
) {
  fun execute(token: String, userId: UUID, userName: String?): Either<AcceptInviteException, AcceptInviteResult> =
    inviteLinkRepository.findByToken(token)
      .mapLeft { AcceptInviteException("Failed to find invite", it) }
      .flatMap { invite ->
        if (invite == null) {
          Either.Left(AcceptInviteException("INVITE_NOT_FOUND"))
        } else {
          validateInvite(invite)
        }
      }
      .flatMap { invite ->
        participantRepository.find(userId, invite.eventId)
          .mapLeft { AcceptInviteException("Failed to check participant", it) }
          .flatMap { existing ->
            if (existing != null) {
              Either.Left(AcceptInviteException("ALREADY_PARTICIPANT:${invite.eventId}"))
            } else {
              Either.Right(invite)
            }
          }
      }
      .flatMap { invite ->
        inviteLinkRepository.acceptInvite(token, userId, userName, invite.eventId)
          .mapLeft { AcceptInviteException("Failed to accept invite", it) }
      }

  private fun validateInvite(invite: InviteLink): Either<AcceptInviteException, InviteLink> = when {
    invite.status == InviteStatus.REVOKED ->
      Either.Left(AcceptInviteException("INVITE_REVOKED"))
    invite.expiresAt.isBefore(clock.instant()) ->
      Either.Left(AcceptInviteException("INVITE_EXPIRED"))
    invite.maxUses != null && invite.currentUses >= invite.maxUses ->
      Either.Left(AcceptInviteException("INVITE_EXHAUSTED"))
    else -> Either.Right(invite)
  }
}
