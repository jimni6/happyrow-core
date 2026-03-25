package com.happyrow.core.domain.participant.get

import arrow.core.Either
import com.happyrow.core.domain.common.model.Page
import com.happyrow.core.domain.common.model.PageRequest
import com.happyrow.core.domain.participant.common.driven.ParticipantRepository
import com.happyrow.core.domain.participant.common.model.Participant
import com.happyrow.core.domain.participant.get.error.GetParticipantsException
import java.util.UUID

class GetParticipantsByEventUseCase(
  private val participantRepository: ParticipantRepository,
) {
  fun execute(eventId: UUID, pageRequest: PageRequest): Either<GetParticipantsException, Page<Participant>> =
    participantRepository.findByEvent(eventId, pageRequest)
      .mapLeft { GetParticipantsException(eventId, it) }
}
