package com.happyrow.core.domain.participant.update

import arrow.core.Either
import arrow.core.flatMap
import com.happyrow.core.domain.participant.common.driven.ParticipantRepository
import com.happyrow.core.domain.participant.common.model.Participant
import com.happyrow.core.domain.participant.update.error.UpdateParticipantException
import com.happyrow.core.domain.participant.update.model.UpdateParticipantRequest

class UpdateParticipantUseCase(
  private val participantRepository: ParticipantRepository,
) {
  fun execute(request: UpdateParticipantRequest): Either<Exception, Participant> = Either.catch {
    participantRepository.find(request.userId, request.eventId)
      .getOrNull() ?: throw IllegalArgumentException("Participant not found")
  }.mapLeft { UpdateParticipantException(request.userId, request.eventId, it) }
    .flatMap { participant ->
      val updatedParticipant = participant.copy(status = request.status)
      participantRepository.update(updatedParticipant)
        .mapLeft { UpdateParticipantException(request.userId, request.eventId, it) }
    }
}
