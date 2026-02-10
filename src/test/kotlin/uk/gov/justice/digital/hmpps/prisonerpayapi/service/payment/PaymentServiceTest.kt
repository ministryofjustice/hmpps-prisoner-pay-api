package uk.gov.justice.digital.hmpps.prisonerpayapi.service.payment

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.prisonerpayapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.payRate
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.payStatusPeriod
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.payment
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.today
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.yesterday
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusType
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.Payment
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.repository.PayRateRepository
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.repository.PayStatusPeriodRepository
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.repository.PaymentRepository
import java.time.Clock
import java.time.LocalDateTime

class PaymentServiceTest {
  val payStatusPeriodRepository: PayStatusPeriodRepository = mock()
  val paymentRepository: PaymentRepository = mock()
  val payRateRepository: PayRateRepository = mock()
  val specialPaymentsService: SpecialPaymentsService = mock()
  val paymentIssuer: PaymentIssuer = mock()
  val clock = Clock.systemDefaultZone()

  val paymentService = PaymentService(
    payStatusPeriodRepository,
    paymentRepository,
    payRateRepository,
    specialPaymentsService,
    paymentIssuer,
    clock,
    2,
  )

  val paymentCaptor = argumentCaptor<Payment>()

  @Test
  fun `should process special payments when there are no previous special payments`() {
    val payStatusPeriodA = payStatusPeriod(prisonerNumber = "A1111AA", startDate = today().minusDays(10))
    val payStatusPeriodB = payStatusPeriod(prisonerNumber = "B2222BB", startDate = today().minusDays(1))

    val yesterday = yesterday()
    val twoDaysAgo = yesterday.minusDays(1)
    val now = LocalDateTime.now()

    whenever(payStatusPeriodRepository.findByPrisonCodeAndDate("RSI", twoDaysAgo)).thenReturn(listOf(payStatusPeriodA))
    whenever(payStatusPeriodRepository.findByPrisonCodeAndDate("RSI", yesterday)).thenReturn(listOf(payStatusPeriodA, payStatusPeriodB))

    val payRateA = payRate(startDate = twoDaysAgo, rate = 60)
    val payRateB = payRate(startDate = twoDaysAgo, rate = 70)

    whenever(payRateRepository.findActivePayRates("RSI", twoDaysAgo)).thenReturn(listOf(payRateA))
    whenever(payRateRepository.findActivePayRates("RSI", yesterday)).thenReturn(listOf(payRateB))

    val paymentsATwoDaysAgo = listOf(
      payment(
        prisonerNumber = "A1111AA",
        eventDate = twoDaysAgo,
        eventPeriod = TimeSlot.AM,
        paymentAmount = 60,
        reference = "aaa",
      ),
      payment(
        prisonerNumber = "A1111AA",
        eventDate = twoDaysAgo,
        eventPeriod = TimeSlot.PM,
        paymentDateTime = now,
        paymentAmount = 70,
        reference = "bbb",
      ),
    )

    whenever(specialPaymentsService.calcPayments(twoDaysAgo, listOf(payStatusPeriodA), mapOf(PayStatusType.LONG_TERM_SICK to payRateA))).thenReturn(paymentsATwoDaysAgo)

    val paymentsAYesterday = listOf(
      payment(
        prisonerNumber = "A1111AA",
        eventPeriod = TimeSlot.AM,
        paymentDateTime = now,
        paymentAmount = 50,
        reference = "ccc",
      ),
      payment(
        prisonerNumber = "A1111AA",
        eventPeriod = TimeSlot.PM,
        paymentDateTime = now,
        paymentAmount = 49,
        reference = "ddd",
      ),
    )

    whenever(specialPaymentsService.calcPayments(yesterday, listOf(payStatusPeriodA), mapOf(PayStatusType.LONG_TERM_SICK to payRateB))).thenReturn(paymentsAYesterday)

    val paymentsBYesterday = listOf(
      payment(
        prisonerNumber = "B2222BB",
        eventPeriod = TimeSlot.AM,
        paymentDateTime = now,
        paymentAmount = 50,
        reference = "eee",
      ),
      payment(
        prisonerNumber = "B2222BB",
        eventPeriod = TimeSlot.PM,
        paymentDateTime = now,
        paymentAmount = 49,
        reference = "fff",
      ),
    )

    whenever(specialPaymentsService.calcPayments(yesterday, listOf(payStatusPeriodB), mapOf(PayStatusType.LONG_TERM_SICK to payRateB))).thenReturn(paymentsBYesterday)

    paymentService.processPayments("RSI")

    verify(paymentIssuer, times(6)).issuePayment(paymentCaptor.capture())

    assertThat(paymentCaptor.allValues).containsExactlyInAnyOrderElementsOf(
      listOf(paymentsATwoDaysAgo + paymentsAYesterday + paymentsBYesterday).flatten(),
    )
  }

  @Test
  fun `should process any new special payments as there are previous special payments`() {
    val payStatusPeriod = payStatusPeriod(prisonerNumber = "A1111AA", startDate = today().minusDays(10))

    val yesterday = yesterday()

    whenever(payStatusPeriodRepository.findByPrisonCodeAndDate("RSI", yesterday)).thenReturn(listOf(payStatusPeriod))

    val oldPayments = listOf(payment())

    whenever(paymentRepository.findPayments("A1111AA", yesterday)).thenReturn(oldPayments)

    paymentService.processPayments("RSI")

    verifyNoInteractions(paymentIssuer)
  }
}
