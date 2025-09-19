package com.happyrow.core.persona

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

object TimePersona {
  val aClock: Clock = Clock.fixed(Instant.parse("2030-02-18T12:00:00.00Z"), ZoneId.systemDefault())
  val now: Instant = aClock.instant()
}
