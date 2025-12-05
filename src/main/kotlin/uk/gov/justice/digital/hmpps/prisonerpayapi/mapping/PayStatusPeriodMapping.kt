package uk.gov.justice.digital.hmpps.prisonerpayapi.mapping

import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request.CreatePayStatusPeriodRequest
import java.time.Clock
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.response.PayStatusPeriod as Dto
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusPeriod as Entity

internal fun Entity.toModel(): Dto = Dto(
  id = id!!,
  prisonerNumber = prisonerNumber,
  type = type,
  startDate = startDate,
  endDate = endDate,
  createdDateTime = createdDateTime,
  createdBy = createdBy,
)

internal fun CreatePayStatusPeriodRequest.toEntity(createdBy: String, clock: Clock) = Entity(
  prisonerNumber = prisonerNumber,
  type = type,
  startDate = startDate,
  endDate = endDate,
  createdBy = createdBy,
  createdDateTime = LocalDateTime.now(clock),
)
