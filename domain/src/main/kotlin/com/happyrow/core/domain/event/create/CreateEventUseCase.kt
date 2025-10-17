package com.happyrow.core.domain.event.create

import arrow.core.Either
import arrow.core.flatMap
import com.happyrow.core.domain.event.common.driven.event.EventRepository
import com.happyrow.core.domain.event.common.model.event.Event
import com.happyrow.core.domain.event.create.error.CreateEventException
import com.happyrow.core.domain.event.create.model.CreateEventRequest
import com.happyrow.core.domain.participant.common.driven.ParticipantRepository
import com.happyrow.core.domain.participant.common.model.ParticipantStatus
import com.happyrow.core.domain.participant.create.model.CreateParticipantRequest
import java.util.UUID

class CreateEventUseCase(
  private val eventRepository: EventRepository,
  private val participantRepository: ParticipantRepository,
) {
  fun create(request: CreateEventRequest): Either<CreateEventException, Event> = eventRepository.create(request)
    .mapLeft { CreateEventException(request, it) }
    .flatMap { event ->
      // Automatically add the event creator as a confirmed participant
      val creatorId = UUID.fromString(request.creator.toString())
      participantRepository.create(
        CreateParticipantRequest(
          userId = creatorId,
          eventId = event.identifier,
          status = ParticipantStatus.CONFIRMED,
        ),
      )
        .map { event } // Return the event, not the participant
        .mapLeft { CreateEventException(request, it) }
    }
}
