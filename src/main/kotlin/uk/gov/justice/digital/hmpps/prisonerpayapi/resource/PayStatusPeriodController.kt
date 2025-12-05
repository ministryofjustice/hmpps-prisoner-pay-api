package uk.gov.justice.digital.hmpps.prisonerpayapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request.CreatePayStatusPeriodRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.response.PayStatusPeriod
import uk.gov.justice.digital.hmpps.prisonerpayapi.service.PayStatusPeriodService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
// @Validated
@RequestMapping(value = ["pay-status-period"], produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(
  name = "Pay Status Period",
)
class PayStatusPeriodController(
  private val payStatusPeriodService: PayStatusPeriodService,
) {
  @PostMapping
  @PreAuthorize("permitAll()") // TODO: Add roles
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Create a new Pay Status Period",
    // description = "Requires role <TODO>",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns the new prisoner status period",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = PayStatusPeriod::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid Request",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Missing required role. Requires the <TODO> role with write scope.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Data not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun create(
    @Valid
    @RequestBody
    @Parameter(description = "The new pay status period", required = true)
    request: CreatePayStatusPeriodRequest,
  ) = payStatusPeriodService.create(request)
}
