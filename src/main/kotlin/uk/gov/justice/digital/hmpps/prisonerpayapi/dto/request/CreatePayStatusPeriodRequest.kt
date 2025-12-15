package uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusType
import java.time.LocalDate

@Schema(description = "The create pay status period request")
data class CreatePayStatusPeriodRequest(
  @field:NotBlank(message = "The prison code")
  @field:Size(max = 3, message = "Prison code must not exceed {max} characters")
  @Schema(description = "The prison code", example = "PVI")
  val prisonCode: String,

  @field:NotBlank(message = "The prisoner number is mandatory")
  @Schema(description = "The prisoner number (NOMIS ID)", example = "A1234AA")
  val prisonerNumber: String,

  @Schema(description = "The pay status type", example = "LONG_TERM_SICK")
  val type: PayStatusType,

  @field:NotNull(message = "The start date is mandatory")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd")
  @Schema(description = "The start date", example = "2025-07-23")
  val startDate: LocalDate,

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd")
  @Schema(description = "The end date", example = "2025-11-14")
  val endDate: LocalDate? = null,
)
