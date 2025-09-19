package com.happyrow.core.infrastructure.technical.config

data class MissionApiConfig(
  val baseUrl: String,
) : ExternalApiConfig(baseUrl)
