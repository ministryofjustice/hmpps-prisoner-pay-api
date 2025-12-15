package uk.gov.justice.digital.hmpps.prisonerpayapi.dto.response

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "Pay Status Period")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PayStatusPeriod(
  @Schema(description = "The id", example = "e6a19788-4f80-4923-8aff-1e5fe26a6139")
  val id: UUID,

  @Schema(description = "The prison code", example = "PVI")
  val prisonCode: String,

  @Schema(description = "The prisoner number (NOMIS ID)", example = "A1234AA")
  val prisonerNumber: String,

  @Schema(description = "The pay status type", example = "LONG_TERM_SICK")
  val type: PayStatusType,

  @Schema(description = "The start date", example = "2025-07-23")
  val startDate: LocalDate,

  @Schema(description = "The end date", example = "2025-09-14")
  val endDate: LocalDate?,

  @Schema(description = "The user who created the pay status period ", example = "USER1")
  val createdBy: String,

  @Schema(description = "The date and time the pay status period was created", example = "2025-07-18T12:45:11")
  val createdDateTime: LocalDateTime,
)
