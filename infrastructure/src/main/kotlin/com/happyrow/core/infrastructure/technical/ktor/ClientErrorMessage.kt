package com.happyrow.core.infrastructure.technical.ktor

const val TECHNICAL_ERROR_TYPE = "TECHNICAL_ERROR"
const val TECHNICAL_ERROR_MESSAGE = "Technical error: An exception has not been mapped into a business exception"

data class ClientErrorMessage(
  @Deprecated("Doesn't respect API guidelines", ReplaceWith("ClientErrorMessage.of()"))
  val message: String,
  val traceId: String? = null,
  val type: String? = null, // TECH-DBT Not nullable once all error responses are migrated
  val detail: String? = null, // TECH-DBT Not nullable once all error responses are migrated
) {

  companion object {
    fun of(type: String, detail: String) = ClientErrorMessage(
      message = detail,
      type = type,
      detail = detail,
    )

    fun technicalErrorMessage() = ClientErrorMessage(
      message = TECHNICAL_ERROR_MESSAGE,
      type = TECHNICAL_ERROR_TYPE,
      detail = TECHNICAL_ERROR_MESSAGE,
    )
  }
}
