package com.happyrow.core.domain.user.findorcreate

import arrow.core.left
import arrow.core.right
import com.happyrow.core.domain.user.common.driven.AppUserRepository
import com.happyrow.core.domain.user.common.model.AppUser
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class FindOrCreateUserUseCaseTestUT {
  private val appUserRepository = mockk<AppUserRepository>()
  private val useCase = FindOrCreateUserUseCase(appUserRepository)

  private val userId = UUID.fromString("ab70634a-345e-415e-8417-60841b6bcb20")
  private val email = "user@example.com"
  private val now = Instant.parse("2026-01-01T00:00:00Z")
  private val appUser = AppUser(id = userId, email = email, createdAt = now, updatedAt = now)

  @BeforeEach
  fun beforeEach() {
    clearAllMocks()
  }

  @Test
  fun `should return user when found or created`() {
    every { appUserRepository.findOrCreate(userId, email) } returns appUser.right()

    val result = useCase.execute(userId, email)

    result shouldBeRight appUser
    verify { appUserRepository.findOrCreate(userId, email) }
  }

  @Test
  fun `should transfer error from repository`() {
    val error = RuntimeException("DB error")
    every { appUserRepository.findOrCreate(userId, email) } returns error.left()

    val result = useCase.execute(userId, email)

    result.shouldBeLeft()
  }
}
