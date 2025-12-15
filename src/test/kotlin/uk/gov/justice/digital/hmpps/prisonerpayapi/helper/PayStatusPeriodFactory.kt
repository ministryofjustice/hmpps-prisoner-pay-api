package uk.gov.justice.digital.hmpps.prisonerpayapi.helper

import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request.CreatePayStatusPeriodRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request.UpdatePayStatusPeriodRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusPeriod
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal val UUID1 = UUID.fromString("11111111-1111-1111-1111-111111111111")

internal fun payStatusPeriod(
  id: UUID? = UUID1,
  prisonCode: String = "PVI",
  prisonerNumber: String = "A1111AA",
  type: PayStatusType = PayStatusType.LONG_TERM_SICK,
  startDate: LocalDate = today(),
  endDate: LocalDate? = today().plusMonths(1),
  createdBy: String = "BLOGGSJ",
  createdDateTime: LocalDateTime = LocalDateTime.now(),
) = PayStatusPeriod(
  id = id,
  prisonCode = prisonCode,
  prisonerNumber = prisonerNumber,
  type = type,
  startDate = startDate,
  endDate = endDate,
  createdBy = createdBy,
  createdDateTime = createdDateTime,
)

internal fun createPayStatusPeriodRequest(
  prisonCode: String = "PVI",
  prisonerNumber: String = "A1111AA",
  type: PayStatusType = PayStatusType.LONG_TERM_SICK,
  startDate: LocalDate = today(),
  endDate: LocalDate? = today().plusMonths(1),
) = CreatePayStatusPeriodRequest(
  prisonCode = prisonCode,
  prisonerNumber = prisonerNumber,
  type = type,
  startDate = startDate,
  endDate = endDate,
)

internal fun updatePayStatusPeriodRequest(
  endDate: LocalDate? = today().plusMonths(2),
  removeEndDate: Boolean = false,
) = UpdatePayStatusPeriodRequest(
  endDate = endDate,
  removeEndDate = removeEndDate,
)
