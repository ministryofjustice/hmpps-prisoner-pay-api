package uk.gov.justice.digital.hmpps.prisonerpayapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.response.PayRateDto
import uk.gov.justice.digital.hmpps.prisonerpayapi.service.PayRateService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@RequestMapping(value = ["pay-rates"], produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "Pay Rates")
@AuthApiResponses
class PayRateController(
  private val payRateService: PayRateService,
) {
  @GetMapping("/prison/{prisonCode}")
  @PreAuthorize("hasRole('ROLE_PRISONER_PAY__PRISONER_PAY_ORCHESTRATOR_API')")
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
}
