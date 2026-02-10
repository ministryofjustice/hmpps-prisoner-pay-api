package uk.gov.justice.digital.hmpps.prisonerpayapi.service.payment

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayRate
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusPeriod
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusType
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.Payment
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class SpecialPaymentsService(
  private val clock: Clock,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun calcPayments(eventDate: LocalDate, payStatusPeriods: List<PayStatusPeriod>, payRates: Map<PayStatusType, PayRate>): List<Payment> {
    if (payStatusPeriods.isEmpty()) return emptyList()

    // TODO: What if there are multiple periods for a prisoner even if it should be impossible?
    val payStatusPeriod = payStatusPeriods.first()

    if (!payStatusPeriod.isActiveOn(eventDate)) return emptyList()

    val payRate = payRates[payStatusPeriod.type]
    if (payRate == null) {
      log.warn("No pay rate found for prisoner ${payStatusPeriod.prisonerNumber} and pay status type ${payStatusPeriod.type}")
      return emptyList()
    }

    return payStatusPeriod
      .applicableSessions()
      .map { timeSlot ->
        Payment(
          prisonCode = payStatusPeriod.prisonCode,
          prisonerNumber = payStatusPeriod.prisonerNumber,
          eventDate = eventDate,
          timeSlot = timeSlot,
          paymentType = payStatusPeriod.type.paymentType,
          paymentDateTime = LocalDateTime.now(clock),
          paymentAmount = payRate.rate,
        )
      }
  }
}
