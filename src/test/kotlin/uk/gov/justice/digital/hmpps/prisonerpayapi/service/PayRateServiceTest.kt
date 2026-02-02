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

  @Test
  fun `should return currently active and future long term sick pay rates`() {
    val expectedEntities = listOf(
      payRate(
        prisonCode = "BCI",
        startDate = LocalDate.of(2026, 1, 25),
        rate = 60,
      ),
      payRate(
        prisonCode = "RSI",
        startDate = LocalDate.of(2026, 1, 10),
        rate = 80,
      ),
    )

    whenever(payRateRepository.findCurrentAndFutureLongTermSickPayRates()).thenReturn(expectedEntities)

    val result = payRateService.getLongTermSickPayRates()

    verify(payRateRepository).findCurrentAndFutureLongTermSickPayRates()

    assertThat(result).hasSize(2)
    assertThat(result.map { it.prisonCode }).containsExactly("BCI", "RSI")
    assertThat(result.map { it.startDate }).containsExactly(LocalDate.of(2026, 1, 25), LocalDate.of(2026, 1, 10))
    assertThat(result.map { it.rate }).containsExactly(60, 80)
  }

  @Test
  fun `should return empty list when no long term sick pay rates exist`() {
    whenever(payRateRepository.findCurrentAndFutureLongTermSickPayRates()).thenReturn(emptyList())

    val result = payRateService.getLongTermSickPayRates()

    verify(payRateRepository).findCurrentAndFutureLongTermSickPayRates()
    assertThat(result).isEmpty()
  }
}
