package com.happyrow.core.domain.invite.common.driven

import arrow.core.Either
import com.happyrow.core.domain.invite.common.error.InviteLinkRepositoryException
import com.happyrow.core.domain.invite.common.model.AcceptInviteResult
import com.happyrow.core.domain.invite.common.model.InviteLink
import com.happyrow.core.domain.invite.common.model.InviteStatus
import com.happyrow.core.domain.invite.create.model.CreateInviteLinkRequest
import java.util.UUID

interface InviteLinkRepository {
  fun create(request: CreateInviteLinkRequest): Either<InviteLinkRepositoryException, InviteLink>
  fun findByToken(token: String): Either<InviteLinkRepositoryException, InviteLink?>
  fun findActiveByEventId(eventId: UUID): Either<InviteLinkRepositoryException, InviteLink?>
  fun updateStatus(token: String, status: InviteStatus): Either<InviteLinkRepositoryException, Unit>
  fun acceptInvite(
    token: String,
    userId: UUID,
    userName: String?,
    eventId: UUID,
  ): Either<InviteLinkRepositoryException, AcceptInviteResult>
  fun countConfirmedParticipants(eventId: UUID): Either<InviteLinkRepositoryException, Long>
}
