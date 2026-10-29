package uk.gov.justice.digital.hmpps.prisonerpayapi.service.paystatusperiod

import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request.CreatePayStatusPeriodRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusPeriod
import java.time.LocalDate

abstract class BasePayStatusPeriodRules(
  private val maxDurationDays: Long? = null,
) : PayStatusPeriodRules {
  override fun validateCreateRequest(request: CreatePayStatusPeriodRequest, today: LocalDate) {
    require(!request.startDate.isBefore(today)) {
      "The start date must not be in the past"
    }
    require(request.endDate == null || !request.endDate.isBefore(today)) {
      "The end date must not be in the past"
    }
    require(request.endDate == null || !request.endDate.isBefore(request.startDate)) {
      "The end date must not be before the start date"
    }
    require(maxDurationDays == null || request.endDate == null || !request.endDate.isAfter(request.startDate.plusDays(maxDurationDays))) {
      "The pay status period must not exceed $maxDurationDays days"
    }
  }

  override fun validateDeleteRequest(existing: PayStatusPeriod, today: LocalDate) {
    require(existing.startDate.isAfter(today)) {
      "Cannot delete a pay status period with a start date in the past or today"
    }
  }
}
