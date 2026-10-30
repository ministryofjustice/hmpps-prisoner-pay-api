package uk.gov.justice.digital.hmpps.prisonerpayapi.service.payrate

import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request.UpdatePayRateRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayRate
import java.time.LocalDate

abstract class BasePayRateRules : PayRateRules {
  override fun validateUpdateRequest(existing: PayRate, request: UpdatePayRateRequest, today: LocalDate) {
    require(request.startDate >= today) {
      "Pay rate start date must not be in the past"
    }
    require(existing.startDate <= today) {
      "Future pay rate must be cancelled before updating"
    }
  }

  override fun validateDeleteRequest(existing: PayRate, today: LocalDate) {
    require(existing.startDate.isAfter(today)) {
      "Cannot delete a pay rate with a start date in the past or today"
    }
  }
}
