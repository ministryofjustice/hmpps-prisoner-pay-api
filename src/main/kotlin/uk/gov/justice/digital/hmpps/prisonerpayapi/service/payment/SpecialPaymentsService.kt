package uk.gov.justice.digital.hmpps.prisonerpayapi.service.payment

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.prisonerpayapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusPeriod
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.Payment
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class SpecialPaymentsService(
  private val clock: Clock,
) {

  fun calcPayments(eventDate: LocalDate, payStatusPeriodForPrisoner: List<PayStatusPeriod>): List<Payment> {
    if (payStatusPeriodForPrisoner.isEmpty()) return emptyList()

    // TODO: What if there are multiple periods for a prisoner even if it should be impossible?
    val payStatusPeriod = payStatusPeriodForPrisoner.first()

    if (!payStatusPeriod.isActiveOn(eventDate)) return emptyList()

    val payments = mutableListOf<Payment>()
    val dayRate = 99 // TODO: Fix this

    calcAmountsPerSession(payStatusPeriod, dayRate)
      .forEach { (timeSlot, amount) ->
        payments.add(
          Payment(
            prisonCode = payStatusPeriod.prisonCode,
            prisonerNumber = payStatusPeriod.prisonerNumber,
            eventDate = eventDate,
            timeSlot = timeSlot,
            paymentType = payStatusPeriod.type.paymentType,
            paymentDateTime = LocalDateTime.now(clock),
            paymentAmount = amount,
          ),
        )
      }

    return payments
  }

  private fun calcAmountsPerSession(payStatusPeriod: PayStatusPeriod, dayRateInPence: Int): Map<TimeSlot, Int> {
    val applicableSessions = payStatusPeriod.applicableSessions()

    val numSessions = applicableSessions.size
    val remainingAmount = dayRateInPence / numSessions
    val firstAmount = remainingAmount + dayRateInPence - (remainingAmount * numSessions)
    val sessionRatesMap = mutableMapOf<TimeSlot, Int>()

    applicableSessions.mapIndexed { index, timeSlot ->
      val amount = when (index) {
        0 -> firstAmount
        else -> remainingAmount
      }
      sessionRatesMap[timeSlot] = amount
    }

    return sessionRatesMap
  }
}
