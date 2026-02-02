package uk.gov.justice.digital.hmpps.prisonerpayapi.dto.response

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Schema(description = "Pay Rate")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PayRateDto(
  @Schema(description = "The id", example = "e6a19788-4f80-4923-8aff-1e5fe26a6139")
  val id: UUID,

  @Schema(description = "The prison code", example = "RSI")
  val prisonCode: String,

  @Schema(description = "The pay status type", example = "LONG_TERM_SICK")
  val type: PayStatusType,

  @Schema(description = "The start date", example = "2025-01-30")
  val startDate: LocalDate,

  @Schema(description = "The day rate in pence", example = "99")
  val rate: Int,

  @Schema(description = "The date and time the pay rate was created", example = "2025-01-26T12:45:11")
  val createdDateTime: LocalDateTime,

  @Schema(description = "The user who created the pay rate", example = "USER1")
  val createdBy: String,
)
