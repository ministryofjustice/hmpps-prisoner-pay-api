package uk.gov.justice.digital.hmpps.prisonerpayapi.helper

import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request.UpdatePayRateRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayRate
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusType
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

const val RISLEY_PRISON_CODE = "RSI"
val clock: Clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("Europe/London"))

internal fun payRate(
  id: UUID? = UUID1,
  prisonCode: String = "RSI",
  type: PayStatusType = PayStatusType.LONG_TERM_SICK,
  startDate: LocalDate = LocalDate.of(2026, 1, 1),
  rate: Int = 99,
  createdBy: String = "TEST_USER",
  createdDateTime: LocalDateTime = LocalDateTime.now(clock),
  updatedBy: String? = null,
  updatedDateTime: LocalDateTime? = null,
) = PayRate(
  id = id,
  prisonCode = prisonCode,
  type = type,
  startDate = startDate,
  rate = rate,
  createdBy = createdBy,
  createdDateTime = createdDateTime,
  updatedBy = updatedBy,
  updatedDateTime = updatedDateTime,
)

internal fun updatePayRateRequest(
  startDate: LocalDate = LocalDate.of(2026, 1, 1),
  rate: Int = 99,
) = UpdatePayRateRequest(
  startDate = startDate,
  rate = rate,
)
