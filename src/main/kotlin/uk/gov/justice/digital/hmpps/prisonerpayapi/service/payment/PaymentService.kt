package uk.gov.justice.digital.hmpps.prisonerpayapi.service.payment

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.repository.PayRateRepository
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.repository.PayStatusPeriodRepository
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.repository.PaymentRepository
import java.time.Clock
import java.time.LocalDate

@Service
class PaymentService(
  private val payStatusPeriodRepository: PayStatusPeriodRepository,
  private val paymentRepository: PaymentRepository,
  private val payRateRepository: PayRateRepository,
  private val specialPaymentsService: SpecialPaymentsService,
  private val paymentIssuer: PaymentIssuer,
  private val clock: Clock,
  @Value($$"${prisoner.pay.api.make.payments.days.back:7}") val daysBack: Long,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun processPayments(prisonCode: String) {
    val today = LocalDate.now(clock)
    val startDate = today.minusDays(daysBack)
    val yesterday = today.minusDays(1)

    log.debug("Determining payments due for {} between {} and {}", prisonCode, startDate, yesterday)

    // For each day in the last n days prior to today...
    startDate.datesUntil(today).forEach { date ->
      // Get prisoners due special payments
      val payStatusPeriodsByPrisonerNumber = payStatusPeriodRepository.findByPrisonCodeAndDate(prisonCode, date).groupBy { it.prisonerNumber }

      val specialPayPrisonerNumbers = payStatusPeriodsByPrisonerNumber.keys

      // TODO: Get prisoners who have attended paid activities
      val activityPrisonerNumbers = setOf<String>()

      val allPrisonerNumbers = specialPayPrisonerNumbers + activityPrisonerNumbers

      if (allPrisonerNumbers.isEmpty()) return@forEach

      // Should be only a single pay rate per type so last wins
      val payRates = payRateRepository.findActivePayRates(prisonCode, date).associateBy { item -> item.type }

      allPrisonerNumbers.forEach { prisonerNumber ->
        // TODO: Find any previous payments
        val oldSpecialPayments = paymentRepository.findPayments(prisonerNumber, date)

        // Calculate new special payments only if there weren't any previous ones
        val specialPayments = if (oldSpecialPayments.isEmpty()) {
          val payStatusPeriodForPrisoner = payStatusPeriodsByPrisonerNumber[prisonerNumber] ?: emptyList()
          specialPaymentsService.calcPayments(date, payStatusPeriodForPrisoner, payRates)
        } else {
          emptyList()
        }

        // TODO: Calculate any activity payments

        // TODO: Calculate any top ups

        // TODO: If any top ups then pay top ups

        val paymentsToProcess = specialPayments

        paymentsToProcess.forEach { payment -> paymentIssuer.issuePayment(payment) }
      }
    }
  }
}
