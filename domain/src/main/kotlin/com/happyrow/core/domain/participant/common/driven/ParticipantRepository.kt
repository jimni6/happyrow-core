package com.happyrow.core.domain.participant.common.driven

import arrow.core.Either
import com.happyrow.core.domain.participant.common.error.CreateParticipantRepositoryException
import com.happyrow.core.domain.participant.common.error.GetParticipantRepositoryException
import com.happyrow.core.domain.participant.common.error.UpdateParticipantRepositoryException
import com.happyrow.core.domain.participant.common.model.Participant
import com.happyrow.core.domain.participant.create.model.CreateParticipantRequest
import java.util.UUID

interface ParticipantRepository {
  fun create(request: CreateParticipantRequest): Either<CreateParticipantRepositoryException, Participant>
  fun update(participant: Participant): Either<UpdateParticipantRepositoryException, Participant>
  fun findOrCreate(userEmail: String, eventId: UUID): Either<CreateParticipantRepositoryException, Participant>
  fun findByEvent(eventId: UUID): Either<GetParticipantRepositoryException, List<Participant>>
  fun find(userEmail: String, eventId: UUID): Either<GetParticipantRepositoryException, Participant?>
}
