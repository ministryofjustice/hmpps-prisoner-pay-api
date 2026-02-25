package uk.gov.justice.digital.hmpps.prisonerpayapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request.UpdatePayRateRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.response.PayRateDto
import uk.gov.justice.digital.hmpps.prisonerpayapi.service.PayRateService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.UUID

@RestController
@RequestMapping(value = ["pay-rates"], produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "Pay Rates")
@AuthApiResponses
class PayRateController(
  private val payRateService: PayRateService,
) {
  @GetMapping("/prison/{prisonCode}")
  @PreAuthorize("hasRole('ROLE_PRISONER_PAY__PRISONER_PAY_ORCHESTRATOR_API')")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Retrieve all current and future pay rates by prison code",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns the list of current and future pay rates by prison code",
        content = [Content(mediaType = "application/json", array = ArraySchema(schema = Schema(implementation = PayRateDto::class)))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid Request",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getCurrentAndFuturePayRates(
    @PathVariable
    @Parameter(description = "The prison code")
    prisonCode: String,
  ): List<PayRateDto> = payRateService.getCurrentAndFuturePayRatesByPrisonCode(prisonCode)

  /**
   * Updates a pay rate.
   *
   * Business rules:
   * - The start date must be today or within the next 30 days.
   * - An existing pay rate with a future start date cannot be updated and must be canceled first.
   * - Only one future pay rate may exist for a given prison code and type.
   * - If the existing pay rate is effective today and the requested start date is today, the pay rate is updated in place.
   * - If the requested start date is in the future, a new pay rate is created depending on whether a future pay rate already exists.
   *   If a future pay rate already exists, 400 is returned, else a new pay rate is created.
   *
   */
  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ROLE_PRISONER_PAY__PRISONER_PAY_UI')")
  @Operation(
    summary = "Update or create the pay rate",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Pay rate updated or created",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = PayRateDto::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid Request",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Pay rate id not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun updatePayRate(
    @PathVariable
    @Parameter(description = "The pay rate id")
    id: UUID,
    @RequestBody request: UpdatePayRateRequest,
  ): PayRateDto = payRateService.update(id, request)
}
