package uk.gov.justice.digital.hmpps.prisonerpayapi.mapping

import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request.UpdatePayRateRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.response.PayRateDto
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayRate
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusType
import java.time.Clock
import java.time.LocalDateTime

internal fun PayRate.toModel(): PayRateDto = PayRateDto(
  id = id!!,
  prisonCode = prisonCode,
  type = type,
  startDate = startDate,
  rate = rate,
  createdDateTime = createdDateTime,
  createdBy = createdBy,
  updatedDateTime = updatedDateTime,
  updatedBy = updatedBy,
)

internal fun UpdatePayRateRequest.toEntity(
  prisonCode: String,
  type: PayStatusType,
  createdBy: String,
  clock: Clock,
) = PayRate(
  prisonCode = prisonCode,
  type = type,
  startDate = startDate,
  rate = rate,
  createdDateTime = LocalDateTime.now(clock),
  createdBy = createdBy,
  updatedDateTime = null,
  updatedBy = null,
)
