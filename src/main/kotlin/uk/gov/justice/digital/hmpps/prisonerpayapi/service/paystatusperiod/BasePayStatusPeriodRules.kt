package uk.gov.justice.digital.hmpps.prisonerpayapi.service.paystatusperiod

import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request.CreatePayStatusPeriodRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request.UpdatePayStatusPeriodRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusPeriod
import java.time.LocalDate

abstract class BasePayStatusPeriodRules(
  private val maxDurationDays: Long? = null,
) : PayStatusPeriodRules {
  companion object {
    fun validateUpdateRequestFields(request: UpdatePayStatusPeriodRequest, today: LocalDate) {
      require(request.endDate == null || !request.removeEndDate) {
        "removeEndDate cannot be true when an endDate is also supplied"
      }
      require(request.endDate == null || !request.endDate.isBefore(today)) {
        "The end date must not be in the past"
      }
    }
  }

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
    validateDuration(request.startDate, request.endDate)
  }

  override fun validateUpdateRequest(existing: PayStatusPeriod, request: UpdatePayStatusPeriodRequest, today: LocalDate) {
    validateUpdateRequestFields(request, today)
    require(existing.endDate?.isBefore(today) != true) {
      "Cannot update an expired pay status period"
    }
    require(request.endDate == null || !request.endDate.isBefore(existing.startDate)) {
      "The end date must not be before the start date"
    }
    validateDuration(existing.startDate, request.endDate)
  }

  private fun validateDuration(startDate: LocalDate, endDate: LocalDate?) {
    require(maxDurationDays == null || endDate == null || !endDate.isAfter(startDate.plusDays(maxDurationDays))) {
      "The pay status period must not exceed $maxDurationDays days"
    }
  }

  override fun validateDeleteRequest(existing: PayStatusPeriod, today: LocalDate) {
    require(existing.startDate.isAfter(today)) {
      "Cannot delete a pay status period with a start date in the past or today"
    }
  }
}
