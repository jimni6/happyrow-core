package com.happyrow.core.domain.event.get

import arrow.core.Either
import com.happyrow.core.domain.event.common.driven.event.EventRepository
import com.happyrow.core.domain.event.common.model.event.Event
import com.happyrow.core.domain.event.creator.model.Creator
import com.happyrow.core.domain.event.get.error.GetEventException

class GetEventsByOrganizerUseCase(
  private val eventRepository: EventRepository,
) {
  fun execute(organizer: Creator): Either<GetEventException, List<Event>> = eventRepository.findByOrganizer(organizer)
}
