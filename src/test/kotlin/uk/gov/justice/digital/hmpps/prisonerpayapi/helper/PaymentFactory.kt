package uk.gov.justice.digital.hmpps.prisonerpayapi.helper

import uk.gov.justice.digital.hmpps.prisonerpayapi.common.PaymentType
import uk.gov.justice.digital.hmpps.prisonerpayapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.Payment
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal fun payment(
  id: UUID? = UUID1,
  prisonCode: String = "RSI",
  prisonerNumber: String = "A1111AA",
  eventDate: LocalDate = yesterday(),
  eventPeriod: TimeSlot = TimeSlot.AM,
  paymentType: PaymentType = PaymentType.LONG_TERM_SICK,
  paymentDateTime: LocalDateTime = LocalDateTime.now(),
  paymentAmount: Int = 50,
  reference: String = "aaa",
) = Payment(
  id = id,
  prisonCode = prisonCode,
  prisonerNumber = prisonerNumber,
  eventDate = eventDate,
  timeSlot = eventPeriod,
  paymentType = paymentType,
  paymentDateTime = paymentDateTime,
  paymentAmount = paymentAmount,
  reference = reference,
)
