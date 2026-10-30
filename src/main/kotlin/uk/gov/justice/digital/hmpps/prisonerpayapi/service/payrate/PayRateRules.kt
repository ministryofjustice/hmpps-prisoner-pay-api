package uk.gov.justice.digital.hmpps.prisonerpayapi.service.payrate

import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request.UpdatePayRateRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayRate
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusType
import java.time.LocalDate

interface PayRateRules {
  fun payType(): PayStatusType

  fun validateUpdateRequest(existing: PayRate, request: UpdatePayRateRequest, today: LocalDate)

  fun validateDeleteRequest(existing: PayRate, today: LocalDate)
}
