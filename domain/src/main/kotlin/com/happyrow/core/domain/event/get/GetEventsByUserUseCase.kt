package com.happyrow.core.domain.event.get

import arrow.core.Either
import com.happyrow.core.domain.event.common.driven.event.EventRepository
import com.happyrow.core.domain.event.common.model.event.Event
import com.happyrow.core.domain.event.get.error.GetEventException

class GetEventsByUserUseCase(
  private val eventRepository: EventRepository,
) {
  fun execute(userEmail: String): Either<GetEventException, List<Event>> = eventRepository.findByUser(userEmail)
}
