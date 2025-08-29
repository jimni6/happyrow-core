package com.happyrow.core.infrastructure.technical.config

import javax.naming.ConfigurationException

private const val URL_SEPARATOR = "/"

sealed class ExternalApiConfig(
  baseUrl: String,
) {
  init {
    if (!baseUrl.endsWith(URL_SEPARATOR)) throw ConfigurationException("$baseUrl must end with '$URL_SEPARATOR'")
  }
}
