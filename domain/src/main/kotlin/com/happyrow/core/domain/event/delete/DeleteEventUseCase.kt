package com.happyrow.core.domain.event.delete

import arrow.core.Either
import com.happyrow.core.domain.event.common.driven.event.EventRepository
import com.happyrow.core.domain.event.delete.error.DeleteEventException
import java.util.UUID

class DeleteEventUseCase(
  private val eventRepository: EventRepository,
) {
  fun delete(identifier: UUID, userId: String): Either<DeleteEventException, Unit> =
    eventRepository.delete(identifier, userId)
      .mapLeft { DeleteEventException(identifier, it) }
}
