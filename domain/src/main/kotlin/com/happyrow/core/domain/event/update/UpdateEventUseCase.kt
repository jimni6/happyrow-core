package com.happyrow.core.domain.event.update

import arrow.core.Either
import com.happyrow.core.domain.event.common.driven.event.EventRepository
import com.happyrow.core.domain.event.common.model.event.Event
import com.happyrow.core.domain.event.update.error.UpdateEventException
import com.happyrow.core.domain.event.update.model.UpdateEventRequest

class UpdateEventUseCase(
  private val eventRepository: EventRepository,
) {
  fun update(request: UpdateEventRequest): Either<UpdateEventException, Event> = eventRepository.update(request)
    .mapLeft { UpdateEventException(request, it) }
}
