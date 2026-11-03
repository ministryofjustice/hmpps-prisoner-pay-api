package uk.gov.justice.digital.hmpps.prisonerpayapi.service.paystatusperiod

import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request.CreatePayStatusPeriodRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request.UpdatePayStatusPeriodRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusPeriod
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusType
import java.time.LocalDate

interface PayStatusPeriodRules {
  fun payType(): PayStatusType

  fun validateCreateRequest(request: CreatePayStatusPeriodRequest, today: LocalDate)

  fun validateUpdateRequest(existing: PayStatusPeriod, request: UpdatePayStatusPeriodRequest)

  fun validateDeleteRequest(existing: PayStatusPeriod, today: LocalDate)
}
