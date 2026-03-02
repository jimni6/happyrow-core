package com.happyrow.core.modules.infrastructure

import com.happyrow.core.modules.infrastructure.driven.httpClientModule
import com.happyrow.core.modules.infrastructure.driven.postgresqlModule
import org.koin.dsl.module

private val drivenModule = module {
  includes(postgresqlModule, httpClientModule)
}

val infrastructureModule = module {
  includes(drivenModule)
}
