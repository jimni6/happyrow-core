package com.happyrow.core.domain.participant.delete

import arrow.core.Either
import arrow.core.flatMap
import com.happyrow.core.domain.event.common.driven.event.EventRepository
import com.happyrow.core.domain.participant.common.driven.ParticipantRepository
import com.happyrow.core.domain.participant.delete.error.DeleteParticipantException
import com.happyrow.core.domain.participant.delete.error.ForbiddenParticipantDeleteException
import com.happyrow.core.domain.participant.update.error.ParticipantNotFoundException
import java.util.UUID

class DeleteParticipantUseCase(
  private val participantRepository: ParticipantRepository,
  private val eventRepository: EventRepository,
) {
  fun execute(userEmail: String, eventId: UUID, authenticatedUserId: String): Either<Exception, Unit> = Either.catch {
    val event = eventRepository.find(eventId).getOrNull()
      ?: throw ParticipantNotFoundException(userEmail, eventId)

    val isOrganizer = event.creator.toString() == authenticatedUserId
    if (!isOrganizer) {
      throw ForbiddenParticipantDeleteException(authenticatedUserId, userEmail, eventId)
    }

    participantRepository.find(userEmail, eventId)
      .getOrNull() ?: throw ParticipantNotFoundException(userEmail, eventId)
  }.mapLeft {
    when (it) {
      is ParticipantNotFoundException -> it
      is ForbiddenParticipantDeleteException -> it
      else -> DeleteParticipantException(userEmail, eventId, it)
    }
  }.flatMap {
    participantRepository.delete(userEmail, eventId)
      .mapLeft { DeleteParticipantException(userEmail, eventId, it) }
  }
}
