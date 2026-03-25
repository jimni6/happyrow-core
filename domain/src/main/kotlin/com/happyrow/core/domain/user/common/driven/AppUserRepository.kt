package com.happyrow.core.domain.user.common.driven

import arrow.core.Either
import com.happyrow.core.domain.user.common.model.AppUser
import java.util.UUID

interface AppUserRepository {
  fun findOrCreate(id: UUID, email: String): Either<Exception, AppUser>
  fun findById(id: UUID): Either<Exception, AppUser?>
}
