package com.happyrow.core.domain.invite.accept.error

class AcceptInviteException(
  message: String,
  cause: Throwable? = null,
) : Exception(message, cause)
