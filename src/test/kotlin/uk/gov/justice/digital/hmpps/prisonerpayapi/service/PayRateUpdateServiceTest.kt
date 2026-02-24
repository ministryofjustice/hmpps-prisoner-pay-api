package uk.gov.justice.digital.hmpps.prisonerpayapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.UUID1
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.payRate
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.updatePayRateRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayRate
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.repository.PayRateRepository
import uk.gov.justice.hmpps.kotlin.auth.HmppsAuthenticationHolder
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Optional
import java.util.UUID

class PayRateUpdateServiceTest {
  val authenticationHolder: HmppsAuthenticationHolder = mock()
  val repository: PayRateRepository = mock()
  val clock: Clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("Europe/London"))

  val payRateUpdateService = PayRateUpdateService(authenticationHolder, repository, clock)
  val today = LocalDate.now(clock)

  @Test
  fun `should update existing pay rate if the existing and request start date is today`() {
    val existing = payRate(startDate = today, rate = 80)
    val request = updatePayRateRequest(startDate = today, rate = 100)

    whenever(repository.findById(UUID1)).thenReturn(Optional.of(existing))
    whenever(authenticationHolder.username).thenReturn("NEW_USER")
    whenever(repository.save(any<PayRate>())).thenAnswer { it.getArgument<PayRate>(0) }

    val result = payRateUpdateService.update(UUID1, request)

    verify(repository).save(existing)

    result.apply {
      assertThat(id).isEqualTo(existing.id)
      assertThat(prisonCode).isEqualTo(existing.prisonCode)
      assertThat(type).isEqualTo(existing.type)
      assertThat(startDate).isEqualTo(existing.startDate)
      assertThat(rate).isEqualTo(request.rate)
      assertThat(createdBy).isEqualTo(existing.createdBy)
      assertThat(createdDateTime).isEqualTo(existing.createdDateTime)
      assertThat(updatedBy).isEqualTo("NEW_USER")
      assertThat(updatedDateTime).isEqualTo(LocalDateTime.now(clock))
    }
  }

  @Test
  fun `should create new pay rate if existing start date is today and request start date is in future`() {
    val existing = payRate(startDate = today, rate = 80)
    val request = updatePayRateRequest(startDate = today.plusDays(10), rate = 100)
    val savedPayRate = payRate(id = UUID.randomUUID(), startDate = request.startDate, rate = request.rate)

    whenever(repository.findById(UUID1)).thenReturn(Optional.of(existing))
    whenever(repository.existsByPrisonCodeAndTypeAndStartDate(any(), any(), any()))
      .thenReturn(false)
    whenever(authenticationHolder.username).thenReturn("TEST_USER")
    whenever(repository.save(any<PayRate>())).thenReturn(savedPayRate)

    val result = payRateUpdateService.update(UUID1, request)

    val captor = argumentCaptor<PayRate>()
    verify(repository).save(captor.capture())
    verify(repository, never()).delete(any())

    with(captor.firstValue) {
      assertThat(id).isNotEqualTo(UUID1)
      assertThat(rate).isEqualTo(request.rate)
      assertThat(startDate).isEqualTo(request.startDate)
    }

    with(result) {
      assertThat(id).isNotEqualTo(UUID1)
      assertThat(prisonCode).isEqualTo(request.prisonCode)
      assertThat(type).isEqualTo(request.type)
      assertThat(startDate).isEqualTo(request.startDate)
      assertThat(rate).isEqualTo(request.rate)
      assertThat(createdBy).isEqualTo("TEST_USER")
      assertThat(createdDateTime).isEqualTo(LocalDateTime.now(clock))
      assertThat(updatedBy).isNull()
      assertThat(updatedDateTime).isNull()
    }
  }

  @Test
  fun `should create new pay rate if existing start date is in past and request start date is today or in future`() {
    val existing = payRate(startDate = today.minusDays(10), rate = 80)
    val request = updatePayRateRequest(startDate = today.plusDays(10), rate = 100)
    val savedPayRate = payRate(id = UUID.randomUUID(), startDate = request.startDate, rate = request.rate)

    whenever(repository.findById(UUID1)).thenReturn(Optional.of(existing))
    whenever(repository.existsByPrisonCodeAndTypeAndStartDate(any(), any(), any()))
      .thenReturn(false)
    whenever(authenticationHolder.username).thenReturn("TEST_USER")
    whenever(repository.save(any<PayRate>())).thenReturn(savedPayRate)

    val result = payRateUpdateService.update(UUID1, request)

    verify(repository).save(any<PayRate>())
    verify(repository, never()).delete(any())

    with(result) {
      assertThat(id).isNotEqualTo(UUID1)
      assertThat(prisonCode).isEqualTo(request.prisonCode)
      assertThat(type).isEqualTo(request.type)
      assertThat(startDate).isEqualTo(request.startDate)
      assertThat(rate).isEqualTo(request.rate)
      assertThat(createdBy).isEqualTo("TEST_USER")
      assertThat(createdDateTime).isEqualTo(LocalDateTime.now(clock))
      assertThat(updatedBy).isNull()
      assertThat(updatedDateTime).isNull()
    }
  }

  @Test
  fun `should delete existing pay rate and create new pay rate if existing and request start date are in future`() {
    val existing = payRate(startDate = today.plusDays(10), rate = 80)
    val request = updatePayRateRequest(startDate = today.plusDays(10), rate = 100)
    val savedPayRate = payRate(id = UUID.randomUUID(), startDate = request.startDate, rate = request.rate)

    whenever(repository.findById(UUID1)).thenReturn(Optional.of(existing))
    whenever(repository.existsByPrisonCodeAndTypeAndStartDate(any(), any(), any()))
      .thenReturn(false)
    whenever(authenticationHolder.username).thenReturn("TEST_USER")
    whenever(repository.save(any<PayRate>())).thenReturn(savedPayRate)

    val result = payRateUpdateService.update(UUID1, request)

    inOrder(repository) {
      verify(repository).delete(existing)
      verify(repository).save(any<PayRate>())
    }

    with(result) {
      assertThat(id).isNotNull()
      assertThat(prisonCode).isEqualTo(request.prisonCode)
      assertThat(type).isEqualTo(request.type)
      assertThat(startDate).isEqualTo(request.startDate)
      assertThat(rate).isEqualTo(request.rate)
      assertThat(createdBy).isEqualTo("TEST_USER")
      assertThat(createdDateTime).isEqualTo(LocalDateTime.now(clock))
      assertThat(updatedBy).isNull()
      assertThat(updatedDateTime).isNull()
    }
  }

  @Test
  fun `should throw exception if pay rate with given id is not found`() {
    val request = updatePayRateRequest(startDate = today.plusDays(10), rate = 100)

    whenever(repository.findById(UUID1)).thenReturn(Optional.empty())

    assertThatThrownBy { payRateUpdateService.update(UUID1, request) }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Pay rate with id '$UUID1' not found")
  }

  @Test
  fun `should throw exception if pay rate already exists for given prison code, type and start date`() {
    val existing = payRate(startDate = today, rate = 80)
    val request = updatePayRateRequest(startDate = today.plusDays(10), rate = 100)

    whenever(repository.findById(UUID1)).thenReturn(Optional.of(existing))
    whenever(repository.existsByPrisonCodeAndTypeAndStartDate(any(), any(), any())).thenReturn(true)

    assertThatThrownBy { payRateUpdateService.update(UUID1, request) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Pay rate already exists for prison: ${request.prisonCode}, type: ${request.type} on ${request.startDate}")
  }
}
