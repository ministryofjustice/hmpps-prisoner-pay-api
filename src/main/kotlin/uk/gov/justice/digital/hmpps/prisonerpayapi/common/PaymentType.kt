package uk.gov.justice.digital.hmpps.prisonerpayapi.common

import io.swagger.v3.oas.annotations.media.Schema

enum class PaymentType {
  @Schema(
    description = "Long Term Sick",
  )
  LONG_TERM_SICK,
}
