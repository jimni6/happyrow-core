package com.happyrow.core.domain.participant.update

import arrow.core.Either
import arrow.core.flatMap
import com.happyrow.core.domain.event.common.driven.event.EventRepository
import com.happyrow.core.domain.participant.common.driven.ParticipantRepository
import com.happyrow.core.domain.participant.common.model.Participant
import com.happyrow.core.domain.participant.update.error.ForbiddenParticipantUpdateException
import com.happyrow.core.domain.participant.update.error.ParticipantNotFoundException
import com.happyrow.core.domain.participant.update.error.UpdateParticipantException
import com.happyrow.core.domain.participant.update.model.UpdateParticipantRequest

class UpdateParticipantUseCase(
  private val participantRepository: ParticipantRepository,
  private val eventRepository: EventRepository,
) {
  fun execute(request: UpdateParticipantRequest, authenticatedEmail: String): Either<Exception, Participant> =
    Either.catch {
      val event = eventRepository.find(request.eventId).getOrNull()
        ?: throw ParticipantNotFoundException(request.userEmail, request.eventId)

      val isOrganizer = event.creator.toString() == authenticatedEmail
      val isSelf = request.userEmail == authenticatedEmail

      if (!isOrganizer && !isSelf) {
        throw ForbiddenParticipantUpdateException(authenticatedEmail, request.userEmail, request.eventId)
      }

      participantRepository.find(request.userEmail, request.eventId)
        .getOrNull() ?: throw ParticipantNotFoundException(request.userEmail, request.eventId)
    }.mapLeft {
      when (it) {
        is ParticipantNotFoundException -> it
        is ForbiddenParticipantUpdateException -> it
        else -> UpdateParticipantException(request.userEmail, request.eventId, it)
      }
    }.flatMap { participant ->
      val updatedParticipant = participant.copy(status = request.status)
      participantRepository.update(updatedParticipant)
        .mapLeft { UpdateParticipantException(request.userEmail, request.eventId, it) }
    }
}
