package com.happyrow.core.domain.event.common

import arrow.core.Either
import arrow.core.flatMap
import com.happyrow.core.domain.event.common.driven.event.EventRepository
import com.happyrow.core.domain.event.common.error.ForbiddenAccessException
import com.happyrow.core.domain.participant.common.driven.ParticipantRepository
import java.util.UUID

class EventAccessControl(
  private val eventRepository: EventRepository,
  private val participantRepository: ParticipantRepository,
) {
  fun assertUserHasAccess(userId: String, userEmail: String, eventId: UUID): Either<Exception, Unit> =
    eventRepository.find(eventId)
      .mapLeft { ForbiddenAccessException(userId, eventId) as Exception }
      .flatMap { event ->
        val isCreator = event.creator.toString() == userId
        if (isCreator) return@flatMap Either.Right(Unit)

        participantRepository.find(userEmail, eventId)
          .mapLeft { ForbiddenAccessException(userId, eventId) as Exception }
          .flatMap { participant ->
            if (participant != null) {
              Either.Right(Unit)
            } else {
              Either.Left(ForbiddenAccessException(userId, eventId))
            }
          }
      }
}
