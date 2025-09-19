package com.happyrow.core.modules.infrastucture

import com.happyrow.core.modules.infrastucture.driven.httpClientModule
import com.happyrow.core.modules.infrastucture.driven.postgresqlModule
import org.koin.dsl.module

private val drivenModule = module {
  includes(postgresqlModule, httpClientModule)
}

val infrastructureModule = module {
  includes(drivenModule)
}
