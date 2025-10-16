package com.happyrow.core.domain.participant.create

import arrow.core.Either
import com.happyrow.core.domain.participant.common.driven.ParticipantRepository
import com.happyrow.core.domain.participant.common.model.Participant
import com.happyrow.core.domain.participant.create.error.CreateParticipantException
import com.happyrow.core.domain.participant.create.model.CreateParticipantRequest

class CreateParticipantUseCase(
  private val participantRepository: ParticipantRepository,
) {
  fun execute(request: CreateParticipantRequest): Either<CreateParticipantException, Participant> =
    participantRepository.create(request)
      .mapLeft { CreateParticipantException(request, it) }
}
