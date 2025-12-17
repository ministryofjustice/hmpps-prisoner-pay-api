package uk.gov.justice.digital.hmpps.prisonerpayapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request.CreatePayStatusPeriodRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request.UpdatePayStatusPeriodRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.response.PayStatusPeriod
import uk.gov.justice.digital.hmpps.prisonerpayapi.service.PayStatusPeriodService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate
import java.util.*

@RestController
// @Validated
@RequestMapping(value = ["pay-status-periods"], produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(
  name = "Pay Status Periods",
)
class PayStatusPeriodController(
  private val payStatusPeriodService: PayStatusPeriodService,
) {
  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ROLE_PRISONER_PAY__PRISONER_PAY_ORCHESTRATOR_API')")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Retrieve a pay status periods by its id",
    // description = "Requires role <TODO>",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns the new prisoner status period",
        content = [Content(mediaType = "application/json", array = ArraySchema(schema = Schema(implementation = PayStatusPeriod::class)))],
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
  fun get(
    @PathVariable
    @Parameter(description = "The id of the of the pay status period")
    id: UUID,
  ) = payStatusPeriodService.getById(id)

  @PostMapping
  @PreAuthorize("permitAll()") // TODO: Add roles
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Create a new pay status period",
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

  @GetMapping
  @PreAuthorize("permitAll()") // TODO: Add roles
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Retrieve a list of pay status periods ordered by start date",
    // description = "Requires role <TODO>",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns the new prisoner status period",
        content = [Content(mediaType = "application/json", array = ArraySchema(schema = Schema(implementation = PayStatusPeriod::class)))],
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
    ],
  )
  fun search(
    @RequestParam(value = "latestStartDate")
    @Parameter(description = "The latest start date the pay status periods started on", example = "2025-07-18")
    latestStartDate: LocalDate,

    @RequestParam(value = "prisonCode")
    @Parameter(description = "a prison code", example = "PVI")
    prisonCode: String? = null,

    @RequestParam(value = "activeOnly", required = false, defaultValue = "true")
    @Parameter(description = "Whether to return results which are currently active, i.e. the end date is null or not before today", example = "true")
    activeOnly: Boolean = true,
  ) = payStatusPeriodService.search(latestStartDate, activeOnly, prisonCode)

  @PatchMapping(value = ["/{id}"])
  @PreAuthorize("permitAll()") // TODO: Add roles
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Update a pay status period",
    // description = "Requires role <TODO>",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Returns the new prisoner status period",
        content = [Content(mediaType = "application/json", array = ArraySchema(schema = Schema(implementation = PayStatusPeriod::class)))],
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
  fun update(
    @PathVariable
    @Parameter(description = "The id of the of the pay status period")
    id: UUID,
    @Valid
    @RequestBody
    @Parameter(description = "The update pay status period", required = true)
    request: UpdatePayStatusPeriodRequest,
  ) = payStatusPeriodService.update(id, request)
}
