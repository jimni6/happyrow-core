package com.happyrow.core.domain.participant.get

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.happyrow.core.domain.common.model.Page
import com.happyrow.core.domain.common.model.PageRequest
import com.happyrow.core.domain.participant.common.driven.ParticipantRepository
import com.happyrow.core.domain.participant.common.error.GetParticipantRepositoryException
import com.happyrow.core.domain.participant.common.model.Participant
import com.happyrow.core.domain.participant.common.model.ParticipantStatus
import com.happyrow.core.domain.participant.get.error.GetParticipantsException
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.util.UUID

class GetParticipantsByEventUseCaseTestUT {
  private val participantRepository = mockk<ParticipantRepository>()
  private val useCase = GetParticipantsByEventUseCase(participantRepository)

  @Test
  fun `should return participants for event`() {
    val eventId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
    val pageRequest = PageRequest(0, 20)
    val aParticipant = Participant(
      identifier = UUID.fromString("11111111-1111-1111-1111-111111111111"),
      userId = UUID.fromString("22222222-2222-2222-2222-222222222222"),
      eventId = eventId,
      status = ParticipantStatus.CONFIRMED,
      joinedAt = java.time.Instant.now(),
      createdAt = java.time.Instant.now(),
      updatedAt = java.time.Instant.now(),
    )

    every { participantRepository.findByEvent(eventId, pageRequest) } returns
      Page.of(listOf(aParticipant), pageRequest, 1L).right()

    val result: Either<GetParticipantsException, Page<Participant>> = useCase.execute(eventId, pageRequest)

    result shouldBeRight Page.of(listOf(aParticipant), pageRequest, 1L)
    verify(exactly = 1) { participantRepository.findByEvent(eventId, pageRequest) }
  }

  @Test
  fun `should transfer error from participant repository`() {
    val eventId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
    val pageRequest = PageRequest(0, 20)
    val cause = RuntimeException("db error")
    val repoError = GetParticipantRepositoryException(eventId, cause)

    every { participantRepository.findByEvent(eventId, pageRequest) } returns repoError.left()

    val result = useCase.execute(eventId, pageRequest)

    val error = result.shouldBeLeft()
    error.shouldBeInstanceOf<GetParticipantsException>()
    (error as GetParticipantsException).eventId shouldBe eventId
  }
}
