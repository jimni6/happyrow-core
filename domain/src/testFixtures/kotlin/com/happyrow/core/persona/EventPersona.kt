package com.happyrow.core.persona

import com.happyrow.core.domain.event.common.model.event.Event
import com.happyrow.core.domain.event.common.model.event.EventType
import com.happyrow.core.domain.event.create.model.CreateEventRequest
import java.time.temporal.ChronoUnit

object EventPersona {
  val Properties = EventPropertiesPersona

  val anEvent = Event(
    identifier = Properties.identifier,
    name = Properties.name,
    description = Properties.description,
    eventDate = Persona.Time.now.plus(5, ChronoUnit.DAYS),
    creationDate = Persona.Time.now.minusSeconds(3600),
    updateDate = Persona.Time.now,
    creator = Persona.User.aUser,
    location = Properties.location,
    type = EventType.DINER,
    members = listOf(Persona.User.aUser),
  )

  val aCreateEventRequest = CreateEventRequest(
    name = Properties.name,
    description = Properties.description,
    eventDate = Persona.Time.now.plus(7, ChronoUnit.DAYS),
    creator = Persona.User.aUser,
    location = Properties.location,
    type = EventType.DINER,
    members = listOf(Persona.User.aUser),
  )
}
