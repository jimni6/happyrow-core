package com.happyrow.core.domain.event.get

import arrow.core.Either
import com.happyrow.core.domain.common.model.Page
import com.happyrow.core.domain.common.model.PageRequest
import com.happyrow.core.domain.event.common.driven.event.EventRepository
import com.happyrow.core.domain.event.common.model.event.Event
import com.happyrow.core.domain.event.get.error.GetEventException

class GetEventsByUserUseCase(
  private val eventRepository: EventRepository,
) {
  fun execute(userId: String, pageRequest: PageRequest): Either<GetEventException, Page<Event>> =
    eventRepository.findByUser(userId, pageRequest)
}
