package uk.gov.justice.digital.hmpps.prisonerpayapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.payRate
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.repository.PayRateRepository
import java.time.LocalDate

class PayRateServiceTest {
  private val payRateRepository: PayRateRepository = mock()
  private val payRateService = PayRateService(payRateRepository)

  private val prisonCode = "RSI"

  @Test
  fun `should return currently active and future pay rates for a given prison`() {
    val payRates = listOf(
      payRate(
        prisonCode = "RSI",
        startDate = LocalDate.of(2026, 2, 1),
        rate = 60,
      ),
      payRate(
        prisonCode = "RSI",
        startDate = LocalDate.of(2026, 3, 1),
        rate = 80,
      ),
      payRate(
        prisonCode = "RSI",
        startDate = LocalDate.of(2026, 4, 1),
        rate = 90,
      ),
    )

    whenever(payRateRepository.getCurrentAndFuturePayRatesByPrisonCode(prisonCode))
      .thenReturn(payRates)

    val result = payRateService.getCurrentAndFuturePayRatesByPrisonCode(prisonCode)

    verify(payRateRepository).getCurrentAndFuturePayRatesByPrisonCode(prisonCode)

    assertThat(result).hasSize(3)
    assertThat(result.map { it.prisonCode }).containsOnly("RSI")
    assertThat(result.map { it.startDate }).containsExactly(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 1))
    assertThat(result.map { it.rate }).containsExactly(60, 80, 90)
    assertThat(result.map { it.createdBy }).containsOnly("TEST_USER")
    assertThat(result.map { it.createdDateTime.toString() }).containsOnly("2026-01-01T10:00")
  }

  @Test
  fun `should return empty list when no pay rates exist`() {
    whenever(payRateRepository.getCurrentAndFuturePayRatesByPrisonCode(prisonCode)).thenReturn(emptyList())

    val result = payRateService.getCurrentAndFuturePayRatesByPrisonCode(prisonCode)

    verify(payRateRepository).getCurrentAndFuturePayRatesByPrisonCode(prisonCode)
    assertThat(result).isEmpty()
  }
}
