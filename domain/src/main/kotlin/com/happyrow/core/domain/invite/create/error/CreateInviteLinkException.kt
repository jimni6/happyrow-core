package com.happyrow.core.domain.invite.create.error

class CreateInviteLinkException(
  message: String,
  cause: Throwable? = null,
) : Exception(message, cause)
