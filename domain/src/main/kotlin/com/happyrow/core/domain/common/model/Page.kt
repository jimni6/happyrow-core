package com.happyrow.core.domain.common.model

data class PageRequest(val page: Int = 0, val size: Int = DEFAULT_PAGE_SIZE) {
  init {
    require(page >= 0) { "Page index must be non-negative" }
    require(size in 1..MAX_PAGE_SIZE) { "Page size must be between 1 and $MAX_PAGE_SIZE" }
  }

  val offset: Long get() = page.toLong() * size

  companion object {
    const val DEFAULT_PAGE_SIZE = 20
    const val MAX_PAGE_SIZE = 100
  }
}

data class Page<T>(
  val content: List<T>,
  val page: Int,
  val size: Int,
  val totalElements: Long,
  val totalPages: Int,
) {
  companion object {
    fun <T> of(content: List<T>, pageRequest: PageRequest, totalElements: Long): Page<T> {
      val totalPages = if (pageRequest.size > 0) {
        ((totalElements + pageRequest.size - 1) / pageRequest.size).toInt()
      } else {
        0
      }
      return Page(
        content = content,
        page = pageRequest.page,
        size = pageRequest.size,
        totalElements = totalElements,
        totalPages = totalPages,
      )
    }
  }
}
