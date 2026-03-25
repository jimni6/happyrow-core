package com.happyrow.core.domain.participant.common.driven

import arrow.core.Either
import com.happyrow.core.domain.common.model.Page
import com.happyrow.core.domain.common.model.PageRequest
import com.happyrow.core.domain.participant.common.error.CreateParticipantRepositoryException
import com.happyrow.core.domain.participant.common.error.DeleteParticipantRepositoryException
import com.happyrow.core.domain.participant.common.error.GetParticipantRepositoryException
import com.happyrow.core.domain.participant.common.error.UpdateParticipantRepositoryException
import com.happyrow.core.domain.participant.common.model.Participant
import com.happyrow.core.domain.participant.create.model.CreateParticipantRequest
import java.util.UUID

interface ParticipantRepository {
  fun create(request: CreateParticipantRequest): Either<CreateParticipantRepositoryException, Participant>
  fun update(participant: Participant): Either<UpdateParticipantRepositoryException, Participant>
  fun delete(userEmail: String, eventId: UUID): Either<DeleteParticipantRepositoryException, Unit>
  fun findOrCreate(userEmail: String, eventId: UUID): Either<CreateParticipantRepositoryException, Participant>
  fun findByEvent(eventId: UUID, pageRequest: PageRequest): Either<GetParticipantRepositoryException, Page<Participant>>
  fun find(userEmail: String, eventId: UUID): Either<GetParticipantRepositoryException, Participant?>
  fun findById(participantId: UUID): Either<GetParticipantRepositoryException, Participant?>
}
