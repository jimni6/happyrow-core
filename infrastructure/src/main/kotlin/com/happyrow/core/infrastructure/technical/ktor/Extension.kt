package com.happyrow.core.infrastructure.technical.ktor

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.happyrow.core.infrastructure.event.common.error.BadRequestException
import io.ktor.server.application.ApplicationCall

fun ApplicationCall.getHeader(name: String): Either<BadRequestException.MissingHeaderException, String> =
  request.headers[name]?.right() ?: BadRequestException.MissingHeaderException(name).left()
