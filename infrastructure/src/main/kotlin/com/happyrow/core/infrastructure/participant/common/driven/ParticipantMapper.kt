package com.happyrow.core.infrastructure.participant.common.driven

import arrow.core.Either
import com.happyrow.core.domain.participant.common.model.Participant
import com.happyrow.core.domain.participant.common.model.ParticipantStatus
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toParticipant(): Either<Throwable, Participant> = Either.catch {
  Participant(
    identifier = this[ParticipantTable.id].value,
    userEmail = this[ParticipantTable.userEmail],
    eventId = this[ParticipantTable.eventId],
    status = ParticipantStatus.valueOf(this[ParticipantTable.status]),
    joinedAt = this[ParticipantTable.joinedAt],
    createdAt = this[ParticipantTable.createdAt],
    updatedAt = this[ParticipantTable.updatedAt],
  )
}
