package com.happyrow.core.modules

import com.happyrow.core.domain.event.create.CreateEventUseCase
import com.happyrow.core.domain.event.get.GetEventsByOrganizerUseCase
import org.koin.dsl.module

val domainModule = module {
  single<CreateEventUseCase> { CreateEventUseCase(get()) }
  single<GetEventsByOrganizerUseCase> { GetEventsByOrganizerUseCase(get()) }
}
