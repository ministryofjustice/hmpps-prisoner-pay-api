package uk.gov.justice.digital.hmpps.prisonerpayapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request.UpdatePayStatusPeriodRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.UUID1
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.payStatusPeriod
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusPeriod
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusType
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.repository.PayStatusPeriodRepository
import uk.gov.justice.digital.hmpps.prisonerpayapi.service.paystatusperiod.PayStatusPeriodRulesRegistry
import uk.gov.justice.digital.hmpps.prisonerpayapi.service.paystatusperiod.rules.LongTermSickPayStatusPeriodRules
import uk.gov.justice.digital.hmpps.prisonerpayapi.service.paystatusperiod.rules.ShortTermSickPayStatusPeriodRules
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

class PayStatusPeriodUpdateServiceTest {
  val repository: PayStatusPeriodRepository = mock()
  val clock: Clock = Clock.fixed(Instant.parse("2026-07-04T12:00:00Z"), ZoneId.of("Europe/London"))

  private fun today(): LocalDate = LocalDate.now(clock)

  val rulesRegistry = PayStatusPeriodRulesRegistry(listOf(LongTermSickPayStatusPeriodRules(), ShortTermSickPayStatusPeriodRules()))
  val service = PayStatusPeriodUpdateService(repository, clock, rulesRegistry)

  val captor = argumentCaptor<PayStatusPeriod>()

  @ParameterizedTest
  @CsvSource(
    "LONG_TERM_SICK, false",
    "LONG_TERM_SICK, true",
    "SHORT_TERM_SICK, false",
    "SHORT_TERM_SICK, true",
  )
  fun `should reject extending or reopening expired periods without changing the entity`(type: PayStatusType, removeEndDate: Boolean) {
    val originalEndDate = today().minusDays(1)
    val entity = payStatusPeriod(type = type, startDate = today().minusDays(10), endDate = originalEndDate)
    whenever(repository.findById(UUID1)).thenReturn(Optional.of(entity))
    val request = UpdatePayStatusPeriodRequest(
      endDate = if (removeEndDate) null else today().plusDays(1),
      removeEndDate = removeEndDate,
    )

    assertThatThrownBy { service.update(UUID1, request) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Cannot update an expired pay status period")

    assertThat(entity.endDate).isEqualTo(originalEndDate)
    verify(repository, never()).saveAndFlush(any())
  }

  @ParameterizedTest
  @CsvSource(
    "LONG_TERM_SICK, -10, 0",
    "LONG_TERM_SICK, -10, 5",
    "LONG_TERM_SICK, 0, 5",
    "LONG_TERM_SICK, 5, 10",
    "LONG_TERM_SICK, -10,",
    "LONG_TERM_SICK, 5,",
    "SHORT_TERM_SICK, -10, 0",
    "SHORT_TERM_SICK, -10, 5",
    "SHORT_TERM_SICK, 0, 5",
    "SHORT_TERM_SICK, 5, 10",
    "SHORT_TERM_SICK, -10,",
    "SHORT_TERM_SICK, 5,",
  )
  fun `should allow setting and removing end dates for active and future periods even at weekends`(
    type: PayStatusType,
    startOffset: Long,
    endOffset: Long?,
  ) {
    val entity = payStatusPeriod(
      type = type,
      startDate = today().plusDays(startOffset),
      endDate = endOffset?.let { today().plusDays(it) },
    )
    whenever(repository.findById(UUID1)).thenReturn(Optional.of(entity))
    whenever(repository.saveAndFlush(entity)).thenReturn(entity)
    val newEndDate = today().plusDays(15)

    assertThat(service.update(UUID1, UpdatePayStatusPeriodRequest(endDate = newEndDate)).endDate).isEqualTo(newEndDate)
    assertThat(service.update(UUID1, UpdatePayStatusPeriodRequest(removeEndDate = true)).endDate).isNull()
  }

  @ParameterizedTest
  @ValueSource(longs = [29, 100])
  fun `should reject an over limit STS update without modifying the entity`(duration: Long) {
    val startDate = today().minusDays(1)
    val originalEndDate = startDate.plusDays(10)
    val entity = payStatusPeriod(type = PayStatusType.SHORT_TERM_SICK, startDate = startDate, endDate = originalEndDate)
    whenever(repository.findById(UUID1)).thenReturn(Optional.of(entity))

    assertThatThrownBy { service.update(UUID1, UpdatePayStatusPeriodRequest(endDate = startDate.plusDays(duration))) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("The pay status period must not exceed 28 days")

    assertThat(entity.endDate).isEqualTo(originalEndDate)
    verify(repository, never()).saveAndFlush(any())
  }

  @ParameterizedTest
  @ValueSource(longs = [0, 27, 28])
  fun `should allow STS updates up to 28 days from the start date`(duration: Long) {
    val startDate = today().plusDays(10)
    val entity = payStatusPeriod(type = PayStatusType.SHORT_TERM_SICK, startDate = startDate, endDate = null)
    whenever(repository.findById(UUID1)).thenReturn(Optional.of(entity))
    whenever(repository.saveAndFlush(entity)).thenReturn(entity)
    val newEndDate = startDate.plusDays(duration)

    assertThat(service.update(UUID1, UpdatePayStatusPeriodRequest(endDate = newEndDate)).endDate).isEqualTo(newEndDate)
    verify(repository).saveAndFlush(entity)
  }

  @Test
  fun `should set end date`() {
    val newEndDate = today().plusDays(100)

    val savedEntity = payStatusPeriod(startDate = today(), endDate = today().plusDays(28))

    whenever(repository.findById(UUID1)).thenReturn(Optional.of(savedEntity))

    whenever(repository.saveAndFlush(any<PayStatusPeriod>())).thenReturn(savedEntity)

    val result = service.update(UUID1, UpdatePayStatusPeriodRequest(endDate = newEndDate))

    assertThat(result.endDate).isEqualTo(newEndDate)

    verify(repository).saveAndFlush(captor.capture())

    assertThat(captor.firstValue.endDate).isEqualTo(newEndDate)
  }

  @ParameterizedTest
  @EnumSource(PayStatusType::class)
  fun `should remove the end date`(type: PayStatusType) {
    val savedEntity = payStatusPeriod(type = type, startDate = today(), endDate = today().plusDays(28))

    whenever(repository.findById(UUID1)).thenReturn(Optional.of(savedEntity))

    whenever(repository.saveAndFlush(any<PayStatusPeriod>())).thenReturn(savedEntity)

    val result = service.update(UUID1, UpdatePayStatusPeriodRequest(removeEndDate = true))

    assertThat(result.endDate).isNull()

    verify(repository).saveAndFlush(captor.capture())

    assertThat(captor.firstValue.endDate).isNull()
  }

  @ParameterizedTest
  @EnumSource(PayStatusType::class)
  fun `should preserve the end date when no change is requested`(type: PayStatusType) {
    val originalEndDate = today().plusDays(28)
    val entity = payStatusPeriod(type = type, startDate = today(), endDate = originalEndDate)
    whenever(repository.findById(UUID1)).thenReturn(Optional.of(entity))
    whenever(repository.saveAndFlush(entity)).thenReturn(entity)

    assertThat(service.update(UUID1, UpdatePayStatusPeriodRequest()).endDate).isEqualTo(originalEndDate)
  }

  @Test
  fun `should throw an exception if removeEndDate and endDate sre set`() {
    assertThatThrownBy {
      service.update(UUID1, UpdatePayStatusPeriodRequest(removeEndDate = true, endDate = today().plusDays(10)))
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("removeEndDate cannot be true when an endDate is also supplied")

    verifyNoInteractions(repository)
  }

  @Test
  fun `should throw an exception if the end date is in the past`() {
    assertThatThrownBy {
      service.update(UUID1, UpdatePayStatusPeriodRequest(endDate = today().minusDays(1)))
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("The end date must not be in the past")

    verifyNoInteractions(repository)
  }

  @Test
  fun `should throw an exception if the end date is before the start date`() {
    val savedEntity = payStatusPeriod(startDate = today().plusDays(30), endDate = null)

    whenever(repository.findById(UUID1)).thenReturn(Optional.of(savedEntity))

    assertThatThrownBy {
      service.update(UUID1, UpdatePayStatusPeriodRequest(endDate = today().plusDays(10)))
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("The end date must not be before the start date")

    verify(repository, never()).saveAndFlush(any())
  }
}
