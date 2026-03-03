package uk.gov.justice.digital.hmpps.prisonerpayapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.RISLEY_PRISON_CODE
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.UUID1
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.payRate
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.updatePayRateRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.repository.PayRateRepository
import uk.gov.justice.digital.hmpps.prisonerpayapi.mapping.toModel
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Optional

class PayRateServiceTest {
  val payRateRepository: PayRateRepository = mock()
  val payRateUpdateService: PayRateUpdateService = mock()
  val clock: Clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("Europe/London"))

  val payRateService = PayRateService(payRateRepository, payRateUpdateService, clock)

  val now: LocalDate = LocalDate.now(clock)

  @Test
  fun `should return currently active and future pay rates for a given prison`() {
    val payRates = listOf(
      payRate(
        startDate = LocalDate.of(2025, 12, 1),
        rate = 60,
      ),
      payRate(
        startDate = LocalDate.of(2026, 3, 1),
        rate = 80,
      ),
      payRate(
        startDate = LocalDate.of(2026, 4, 1),
        rate = 90,
      ),
    )

    whenever(payRateRepository.getCurrentAndFuturePayRatesByPrisonCode(RISLEY_PRISON_CODE, now))
      .thenReturn(payRates)

    val result = payRateService.getCurrentAndFuturePayRatesByPrisonCode(RISLEY_PRISON_CODE)

    verify(payRateRepository).getCurrentAndFuturePayRatesByPrisonCode(RISLEY_PRISON_CODE, now)

    with(result) {
      assertThat(this).hasSize(3)
      assertThat(map { it.prisonCode }).containsOnly(RISLEY_PRISON_CODE)
      assertThat(map { it.startDate }).containsExactly(
        LocalDate.of(2025, 12, 1),
        LocalDate.of(2026, 3, 1),
        LocalDate.of(2026, 4, 1),
      )
      assertThat(map { it.rate }).containsExactly(60, 80, 90)
      assertThat(map { it.createdBy }).containsOnly("TEST_USER")
      assertThat(map { it.createdDateTime }).containsOnly(LocalDateTime.now(clock))
    }
  }

  @Test
  fun `should return empty list when no pay rates exist`() {
    whenever(payRateRepository.getCurrentAndFuturePayRatesByPrisonCode(RISLEY_PRISON_CODE, now)).thenReturn(emptyList())

    val result = payRateService.getCurrentAndFuturePayRatesByPrisonCode(RISLEY_PRISON_CODE)

    verify(payRateRepository).getCurrentAndFuturePayRatesByPrisonCode(RISLEY_PRISON_CODE, now)
    assertThat(result).isEmpty()
  }

  @Test
  fun `should call PayRateUpdateService to update pay rate`() {
    val request = updatePayRateRequest(startDate = now, rate = 100)
    val newPayRate = payRate(startDate = request.startDate, rate = request.rate)

    whenever(payRateUpdateService.update(UUID1, request)).thenReturn(newPayRate.toModel())

    val result = payRateService.update(UUID1, request)

    verify(payRateUpdateService).update(UUID1, request)
    assertThat(result).isEqualTo(newPayRate.toModel())
  }

  @Test
  fun `should delete a future pay rate`() {
    val futurePayRate = payRate(id = UUID1, startDate = now.plusDays(10))

    whenever(payRateRepository.findById(UUID1)).thenReturn(Optional.of(futurePayRate))

    payRateService.delete(UUID1)

    verify(payRateRepository).findById(UUID1)
    verify(payRateRepository).delete(futurePayRate)
  }

  @Test
  fun `should throw exception if the pay rate start date is in the past`() {
    val futurePayRate = payRate(id = UUID1, startDate = now.minusDays(10))

    whenever(payRateRepository.findById(UUID1)).thenReturn(Optional.of(futurePayRate))

    assertThatThrownBy { payRateService.delete(UUID1) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Cannot delete a pay rate with a start date in the past or today")
  }

  @Test
  fun `should throw exception if the pay rate start date is today`() {
    val futurePayRate = payRate(id = UUID1, startDate = now)

    whenever(payRateRepository.findById(UUID1)).thenReturn(Optional.of(futurePayRate))

    assertThatThrownBy { payRateService.delete(UUID1) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Cannot delete a pay rate with a start date in the past or today")
  }

  @Test
  fun `should throw exception if a pay rate does not exist`() {
    whenever(payRateRepository.findById(UUID1)).thenReturn(java.util.Optional.empty())

    assertThatThrownBy { payRateService.delete(UUID1) }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Pay Rate $UUID1 not found")
  }
}
