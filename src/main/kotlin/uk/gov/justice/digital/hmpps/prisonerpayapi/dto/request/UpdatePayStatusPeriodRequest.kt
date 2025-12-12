package uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "The update pay status period request")
data class UpdatePayStatusPeriodRequest(
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd")
  @Schema(description = "The end date", example = "2025-11-14")
  val endDate: LocalDate? = null,

  @Schema(description = "Should the end date be removed? Can only be true if the end date is null", example = "true", defaultValue = "false")
  val removeEndDate: Boolean = false,
)
