package com.happyrow.core.domain.event.create

import arrow.core.Either
import com.happyrow.core.domain.event.common.driven.event.EventRepository
import com.happyrow.core.domain.event.common.model.event.Event
import com.happyrow.core.domain.event.create.error.CreateEventException
import com.happyrow.core.domain.event.create.model.CreateEventRequest

class CreateEventUseCase(
  private val eventRepository: EventRepository,
) {
  fun create(request: CreateEventRequest): Either<CreateEventException, Event> = eventRepository.create(request)
    .mapLeft { CreateEventException(request, it) }
}
