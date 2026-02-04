package com.happyrow.core.domain.contribution.reduce.error

data class InsufficientContributionException(
  val currentQuantity: Int,
  val requestedReduction: Int,
) : Exception("Cannot reduce by $requestedReduction. Current contribution is only $currentQuantity")
