package uk.gov.justice.digital.hmpps.prisonerpayapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.RISLEY_PRISON_CODE
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.UUID1
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.payRate
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.updatePayRateRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusType
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.repository.PayRateRepository
import uk.gov.justice.digital.hmpps.prisonerpayapi.mapping.toModel
import uk.gov.justice.digital.hmpps.prisonerpayapi.service.payrate.PayRateRules
import uk.gov.justice.digital.hmpps.prisonerpayapi.service.payrate.PayRateRulesRegistry
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

  val rules: PayRateRules = mock()
  val rulesRegistry: PayRateRulesRegistry = mock()
  val payRateService = PayRateService(payRateRepository, payRateUpdateService, clock, rulesRegistry)

  val now: LocalDate = LocalDate.now(clock)

  @BeforeEach
  fun setupRules() {
    whenever(rulesRegistry.rulesFor(PayStatusType.LONG_TERM_SICK)).thenReturn(rules)
  }

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
  fun `should validate using the selected strategy before deleting a pay rate`() {
    val futurePayRate = payRate(id = UUID1, startDate = now.plusDays(10))

    whenever(payRateRepository.findById(UUID1)).thenReturn(Optional.of(futurePayRate))

    payRateService.delete(UUID1)

    inOrder(payRateRepository, rulesRegistry, rules) {
      verify(payRateRepository).findById(UUID1)
      verify(rulesRegistry).rulesFor(futurePayRate.type)
      verify(rules).validateDeleteRequest(futurePayRate, now)
      verify(payRateRepository).delete(futurePayRate)
    }
  }

  @Test
  fun `should not delete when the selected strategy rejects deletion`() {
    val existing = payRate(id = UUID1, startDate = now.plusDays(1))
    whenever(payRateRepository.findById(UUID1)).thenReturn(Optional.of(existing))
    doThrow(IllegalArgumentException("Deletion rejected by strategy"))
      .whenever(rules).validateDeleteRequest(existing, now)

    assertThatThrownBy { payRateService.delete(UUID1) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Deletion rejected by strategy")

    verify(rules).validateDeleteRequest(existing, now)
    verify(payRateRepository, never()).delete(any())
  }

  @Test
  fun `should throw exception if a pay rate does not exist`() {
    whenever(payRateRepository.findById(UUID1)).thenReturn(java.util.Optional.empty())

    assertThatThrownBy { payRateService.delete(UUID1) }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Pay Rate $UUID1 not found")
  }
}
