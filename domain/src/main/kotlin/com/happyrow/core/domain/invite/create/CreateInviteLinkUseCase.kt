package com.happyrow.core.domain.invite.create

import arrow.core.Either
import arrow.core.flatMap
import com.happyrow.core.domain.event.common.driven.event.EventRepository
import com.happyrow.core.domain.invite.common.driven.InviteLinkRepository
import com.happyrow.core.domain.invite.common.model.InviteLink
import com.happyrow.core.domain.invite.create.error.CreateInviteLinkException
import com.happyrow.core.domain.invite.create.model.CreateInviteLinkRequest

class CreateInviteLinkUseCase(
  private val inviteLinkRepository: InviteLinkRepository,
  private val eventRepository: EventRepository,
) {
  fun execute(request: CreateInviteLinkRequest): Either<CreateInviteLinkException, InviteLink> =
    eventRepository.find(request.eventId)
      .mapLeft { CreateInviteLinkException("Event not found: ${request.eventId}", it) }
      .flatMap { event ->
        if (event.creator.value.toString() != request.createdBy.toString()) {
          Either.Left(CreateInviteLinkException("Only the event organizer can generate an invite link"))
        } else {
          Either.Right(event)
        }
      }
      .flatMap {
        inviteLinkRepository.findActiveByEventId(request.eventId)
          .mapLeft { CreateInviteLinkException("Failed to check for existing invite", it) }
      }
      .flatMap { existingInvite ->
        if (existingInvite != null) {
          Either.Left(CreateInviteLinkException("An active invite link already exists for this event"))
        } else {
          Either.Right(Unit)
        }
      }
      .flatMap {
        inviteLinkRepository.create(request)
          .mapLeft { CreateInviteLinkException("Failed to create invite link", it) }
      }
}
