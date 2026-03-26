package com.happyrow.core.domain.invite.validate

import arrow.core.Either
import arrow.core.flatMap
import com.happyrow.core.domain.event.common.driven.event.EventRepository
import com.happyrow.core.domain.invite.common.driven.InviteLinkRepository
import com.happyrow.core.domain.invite.common.model.InviteEventSummary
import com.happyrow.core.domain.invite.common.model.InviteLink
import com.happyrow.core.domain.invite.common.model.InviteStatus
import com.happyrow.core.domain.invite.common.model.InviteValidation
import com.happyrow.core.domain.invite.common.model.InviteValidationStatus
import com.happyrow.core.domain.user.common.driven.AppUserRepository
import java.time.Clock

class ValidateInviteTokenUseCase(
  private val inviteLinkRepository: InviteLinkRepository,
  private val eventRepository: EventRepository,
  private val appUserRepository: AppUserRepository,
  private val clock: Clock,
) {
  fun execute(token: String): Either<Exception, InviteValidation?> = inviteLinkRepository.findByToken(token)
    .mapLeft { it as Exception }
    .flatMap { inviteLink ->
      if (inviteLink == null) {
        Either.Right(null)
      } else {
        buildValidation(inviteLink)
      }
    }

  private fun buildValidation(invite: InviteLink): Either<Exception, InviteValidation> {
    val status = computeStatus(invite)

    if (status != InviteValidationStatus.VALID) {
      return Either.Right(
        InviteValidation(
          token = invite.token,
          status = status,
          event = null,
          expiresAt = invite.expiresAt,
        ),
      )
    }

    return eventRepository.find(invite.eventId)
      .mapLeft { it as Exception }
      .flatMap { event ->
        val organizerName = appUserRepository.findById(event.creator.value)
          .fold({ null }, { it?.name }) ?: "Organizer"

        inviteLinkRepository.countConfirmedParticipants(invite.eventId)
          .mapLeft { it as Exception }
          .map { participantCount ->
            InviteValidation(
              token = invite.token,
              status = InviteValidationStatus.VALID,
              event = InviteEventSummary(
                identifier = event.identifier,
                name = event.name,
                eventDate = event.eventDate,
                location = event.location,
                type = event.type,
                organizerName = organizerName,
                participantCount = participantCount,
              ),
              expiresAt = invite.expiresAt,
            )
          }
      }
  }

  private fun computeStatus(invite: InviteLink): InviteValidationStatus = when {
    invite.status == InviteStatus.REVOKED -> InviteValidationStatus.REVOKED
    invite.expiresAt.isBefore(clock.instant()) -> InviteValidationStatus.EXPIRED
    invite.maxUses != null && invite.currentUses >= invite.maxUses -> InviteValidationStatus.EXHAUSTED
    else -> InviteValidationStatus.VALID
  }
}
