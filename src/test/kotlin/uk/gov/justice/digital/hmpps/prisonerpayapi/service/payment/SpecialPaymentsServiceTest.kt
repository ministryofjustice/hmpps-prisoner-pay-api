package uk.gov.justice.digital.hmpps.prisonerpayapi.service.payment

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.prisonerpayapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.payStatusPeriod
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class SpecialPaymentsServiceTest {
  val clock: Clock = Clock.fixed(Instant.parse("2025-07-23T12:34:56Z"), ZoneId.of("Europe/London"))

  val specialPaymentsService = SpecialPaymentsService(clock)

  @Test
  fun `should calculate special payments for a prisoner`() {
    val eventDate = LocalDate.of(2025, 1, 16)

    val payStatusPeriod = payStatusPeriod(startDate = eventDate.minusDays(1), endDate = null)

    val result = specialPaymentsService.calcPayments(eventDate, listOf(payStatusPeriod))

    assertThat(result).hasSize(2)

    with(result[0]) {
      assertThat(prisonCode).isEqualTo(payStatusPeriod.prisonCode)
      assertThat(prisonerNumber).isEqualTo(payStatusPeriod.prisonerNumber)
      assertThat(eventDate).isEqualTo(eventDate)
      assertThat(timeSlot).isEqualTo(TimeSlot.AM)
      assertThat(paymentType).isEqualTo(payStatusPeriod.type.paymentType)
      assertThat(paymentDateTime).isEqualTo(LocalDateTime.now(clock))
      assertThat(paymentAmount).isEqualTo(50)
    }

    with(result[1]) {
      assertThat(prisonCode).isEqualTo(payStatusPeriod.prisonCode)
      assertThat(prisonerNumber).isEqualTo(payStatusPeriod.prisonerNumber)
      assertThat(eventDate).isEqualTo(eventDate)
      assertThat(timeSlot).isEqualTo(TimeSlot.PM)
      assertThat(paymentType).isEqualTo(payStatusPeriod.type.paymentType)
      assertThat(paymentDateTime).isEqualTo(LocalDateTime.now(clock))
      assertThat(paymentAmount).isEqualTo(49)
    }
  }

  @Test
  fun `should return no payments if there are no special payments for the prisoner`() {
    val eventDate = LocalDate.of(2025, 1, 16)

    val result = specialPaymentsService.calcPayments(eventDate, emptyList())

    assertThat(result).isEmpty()
  }

  @Test
  fun `should use first pay status period if multiple exist`() {
    val eventDate = LocalDate.of(2025, 1, 16)

    val payStatusPeriod1 = payStatusPeriod(startDate = eventDate.plusDays(1), endDate = null)
    val payStatusPeriod2 = payStatusPeriod(startDate = eventDate.minusDays(1), endDate = null)

    val result = specialPaymentsService.calcPayments(eventDate, listOf(payStatusPeriod1, payStatusPeriod2))

    assertThat(result).isEmpty()
  }

  @Test
  fun `should no payments if the special payment is not active on the date`() {
    val eventDate = LocalDate.of(2025, 1, 18) // Weekend

    val payStatusPeriod = payStatusPeriod(startDate = eventDate.plusDays(1), endDate = null)

    val result = specialPaymentsService.calcPayments(eventDate, listOf(payStatusPeriod))

    assertThat(result).isEmpty()
  }
}
