package com.happyrow.core.domain.invite.getactive

import arrow.core.Either
import arrow.core.flatMap
import com.happyrow.core.domain.event.common.driven.event.EventRepository
import com.happyrow.core.domain.invite.common.driven.InviteLinkRepository
import com.happyrow.core.domain.invite.common.model.InviteLink
import java.util.UUID

class GetActiveInviteLinkUseCase(
  private val inviteLinkRepository: InviteLinkRepository,
  private val eventRepository: EventRepository,
) {
  fun execute(eventId: UUID, userId: String): Either<Exception, InviteLink?> = eventRepository.find(eventId)
    .mapLeft { it as Exception }
    .flatMap { event ->
      if (event.creator.value.toString() != userId) {
        Either.Left(ForbiddenInviteAccessException(userId, eventId))
      } else {
        inviteLinkRepository.findActiveByEventId(eventId)
          .mapLeft { it as Exception }
      }
    }
}

class ForbiddenInviteAccessException(
  val userId: String,
  val eventId: UUID,
) : Exception("User $userId is not the organizer of event $eventId")
