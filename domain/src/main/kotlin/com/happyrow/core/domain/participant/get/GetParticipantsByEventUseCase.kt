package com.happyrow.core.domain.participant.get

import arrow.core.Either
import com.happyrow.core.domain.participant.common.driven.ParticipantRepository
import com.happyrow.core.domain.participant.common.model.Participant
import com.happyrow.core.domain.participant.get.error.GetParticipantsException
import java.util.UUID

class GetParticipantsByEventUseCase(
  private val participantRepository: ParticipantRepository,
) {
  fun execute(eventId: UUID): Either<GetParticipantsException, List<Participant>> =
    participantRepository.findByEvent(eventId)
      .mapLeft { GetParticipantsException(eventId, it) }
}
