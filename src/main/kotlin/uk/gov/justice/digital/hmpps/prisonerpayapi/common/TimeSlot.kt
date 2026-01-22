package uk.gov.justice.digital.hmpps.prisonerpayapi.common

import io.swagger.v3.oas.annotations.media.Schema

enum class TimeSlot {
  @Schema(
    description = "Morning",
  )
  AM,

  @Schema(
    description = "Afternoon",
  )
  PM,

  @Schema(
    description = "Evening",
  )
  ED,
}
