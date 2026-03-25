package com.happyrow.core.persona

import com.happyrow.core.domain.event.creator.model.Creator
import java.util.UUID

object UserPersona {
  val aUser = Creator(UUID.fromString("ab70634a-345e-415e-8417-60841b6bcb20"))
  const val aUserEmail = "user@example.com"
  val aRequesterUserId = "123455"
}
