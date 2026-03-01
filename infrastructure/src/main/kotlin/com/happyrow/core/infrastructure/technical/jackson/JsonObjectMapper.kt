package com.happyrow.core.infrastructure.technical.jackson

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule

object JsonObjectMapper {
  val defaultMapper: ObjectMapper by lazy {
    ObjectMapper()
      .setConfig()
  }
}

fun ObjectMapper.setConfig(): ObjectMapper {
  registerModule(
    KotlinModule.Builder()
      .configure(KotlinFeature.StrictNullChecks, true)
      .build(),
  )
  registerModule(JavaTimeModule())
  configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)
  setSerializationInclusion(JsonInclude.Include.NON_NULL)
  configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false)
  configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false)
  configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)
  propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
  return this
}
