package com.happyrow.core.domain.event.common.driven.event

import arrow.core.Either
import com.happyrow.core.domain.event.common.error.CreateEventRepositoryException
import com.happyrow.core.domain.event.common.error.DeleteEventRepositoryException
import com.happyrow.core.domain.event.common.error.UpdateEventRepositoryException
import com.happyrow.core.domain.event.common.model.event.Event
import com.happyrow.core.domain.event.create.model.CreateEventRequest
import com.happyrow.core.domain.event.creator.model.Creator
import com.happyrow.core.domain.event.get.error.GetEventException
import com.happyrow.core.domain.event.update.model.UpdateEventRequest
import java.util.UUID

interface EventRepository {
  fun create(request: CreateEventRequest): Either<CreateEventRepositoryException, Event>
  fun update(request: UpdateEventRequest): Either<UpdateEventRepositoryException, Event>
  fun delete(identifier: UUID, userId: String): Either<DeleteEventRepositoryException, Unit>
  fun find(identifier: UUID): Either<GetEventException, Event>
  fun findByOrganizer(organizer: Creator): Either<GetEventException, List<Event>>
}
