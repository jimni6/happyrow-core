package com.happyrow.core.domain.event.common.driven.event

import arrow.core.Either
import com.happyrow.core.domain.event.common.error.CreateEventRepositoryException
import com.happyrow.core.domain.event.common.model.event.Event
import com.happyrow.core.domain.event.create.error.GetEventException
import com.happyrow.core.domain.event.create.model.CreateEventRequest
import java.util.UUID

interface EventRepository {
  fun create(request: CreateEventRequest): Either<CreateEventRepositoryException, Event>
  fun find(identifier: UUID): Either<GetEventException, Event>
}
