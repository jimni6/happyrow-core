package com.happyrow.core.domain.participant.common.driven

import arrow.core.Either
import com.happyrow.core.domain.participant.common.error.CreateParticipantRepositoryException
import com.happyrow.core.domain.participant.common.error.GetParticipantRepositoryException
import com.happyrow.core.domain.participant.common.model.Participant
import com.happyrow.core.domain.participant.create.model.CreateParticipantRequest
import java.util.UUID

interface ParticipantRepository {
  fun create(request: CreateParticipantRequest): Either<CreateParticipantRepositoryException, Participant>
  fun findOrCreate(userId: UUID, eventId: UUID): Either<CreateParticipantRepositoryException, Participant>
  fun findByEvent(eventId: UUID): Either<GetParticipantRepositoryException, List<Participant>>
  fun find(userId: UUID, eventId: UUID): Either<GetParticipantRepositoryException, Participant?>
}
