package uk.gov.justice.digital.hmpps.prisonerpayapi.mapping

import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.response.PayRateDto
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayRate

internal fun PayRate.toModel(): PayRateDto = PayRateDto(
  id = id!!,
  prisonCode = prisonCode,
  type = type,
  startDate = startDate,
  rate = rate,
  createdDateTime = createdDateTime,
  createdBy = createdBy,
)
