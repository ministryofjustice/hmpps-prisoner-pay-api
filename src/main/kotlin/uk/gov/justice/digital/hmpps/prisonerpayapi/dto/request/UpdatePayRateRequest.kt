package uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusType
import java.time.LocalDate

@Schema(description = "The update or create pay rate request")
data class UpdatePayRateRequest(
  @Schema(description = "The prison code", example = "RSI")
  val prisonCode: String,

  @Schema(description = "The pay status type", example = "LONG_TERM_SICK")
  val type: PayStatusType,

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd")
  @Schema(description = "The start date", example = "2025-01-01")
  val startDate: LocalDate,

  @Schema(description = "The day rate in pence per session", example = "99")
  val rate: Int,
)
