package com.happyrow.core.modules

import com.happyrow.core.domain.event.create.CreateEventUseCase
import com.happyrow.core.domain.event.delete.DeleteEventUseCase
import com.happyrow.core.domain.event.get.GetEventsByOrganizerUseCase
import com.happyrow.core.domain.event.update.UpdateEventUseCase
import org.koin.dsl.module

val domainModule = module {
  single<CreateEventUseCase> { CreateEventUseCase(get()) }
  single<GetEventsByOrganizerUseCase> { GetEventsByOrganizerUseCase(get()) }
  single<UpdateEventUseCase> { UpdateEventUseCase(get()) }
  single<DeleteEventUseCase> { DeleteEventUseCase(get()) }
}
