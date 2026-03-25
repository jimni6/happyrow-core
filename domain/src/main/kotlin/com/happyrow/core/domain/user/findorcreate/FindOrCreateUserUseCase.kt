package com.happyrow.core.domain.user.findorcreate

import arrow.core.Either
import com.happyrow.core.domain.user.common.driven.AppUserRepository
import com.happyrow.core.domain.user.common.model.AppUser
import java.util.UUID

class FindOrCreateUserUseCase(
  private val appUserRepository: AppUserRepository,
) {
  fun execute(userId: UUID, email: String): Either<Exception, AppUser> = appUserRepository.findOrCreate(userId, email)
}
