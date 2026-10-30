package uk.gov.justice.digital.hmpps.prisonerpayapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request.UpdatePayStatusPeriodRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.UUID1
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.createPayStatusPeriodRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.payStatusPeriod
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.today
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusPeriod
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusType
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.repository.PayStatusPeriodRepository
import uk.gov.justice.digital.hmpps.prisonerpayapi.mapping.toModel
import uk.gov.justice.digital.hmpps.prisonerpayapi.service.paystatusperiod.PayStatusPeriodRules
import uk.gov.justice.digital.hmpps.prisonerpayapi.service.paystatusperiod.PayStatusPeriodRulesRegistry
import uk.gov.justice.hmpps.kotlin.auth.HmppsAuthenticationHolder
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

class PayStatusPeriodServiceTest {
  val authenticationHolder: HmppsAuthenticationHolder = mock()
  val repository: PayStatusPeriodRepository = mock()
  val updateService: PayStatusPeriodUpdateService = mock()
  val clock: Clock = Clock.fixed(Instant.parse("2025-07-23T12:34:56Z"), ZoneId.of("Europe/London"))

  val rules: PayStatusPeriodRules = mock()
  val rulesRegistry: PayStatusPeriodRulesRegistry = mock()

  val payStatusPeriodService = PayStatusPeriodService(authenticationHolder, repository, updateService, clock, rulesRegistry)

  val captor = argumentCaptor<PayStatusPeriod>()
  val today = LocalDate.now(clock)

  @BeforeEach
  fun setUp() {
    whenever(authenticationHolder.username).thenReturn("BLOGGSJ")
    whenever(rulesRegistry.rulesFor(PayStatusType.LONG_TERM_SICK)).thenReturn(rules)
  }

  @Test
  fun `should save pay status period with start date of today`() {
    val request = createPayStatusPeriodRequest()

    val newEntity = payStatusPeriod(
      id = null,
      prisonCode = request.prisonCode,
      prisonerNumber = request.prisonerNumber,
      type = request.type,
      startDate = request.startDate,
      endDate = request.endDate,
      createdBy = "BLOGGSJ",
      createdDateTime = LocalDateTime.now(clock),
    )

    val savedEntity = payStatusPeriod(
      id = UUID.randomUUID(),
      prisonCode = newEntity.prisonCode,
      prisonerNumber = newEntity.prisonerNumber,
      type = newEntity.type,
      startDate = newEntity.startDate,
      endDate = newEntity.endDate,
      createdBy = newEntity.createdBy,
      createdDateTime = newEntity.createdDateTime,
    )

    whenever(repository.save(any<PayStatusPeriod>())).thenReturn(savedEntity)

    val result = payStatusPeriodService.create(request)

    assertThat(result).isEqualTo(savedEntity.toModel())

    inOrder(repository, rulesRegistry, rules) {
      verify(rulesRegistry).rulesFor(request.type)
      verify(rules).validateCreateRequest(request, today)
      verify(repository).save(any())
    }

    verify(repository).save(captor.capture())

    assertThat(captor.firstValue).usingRecursiveComparison().isEqualTo(newEntity)
  }

  @Test
  fun `should not save when the selected strategy rejects create`() {
    val request = createPayStatusPeriodRequest()
    doThrow(IllegalArgumentException("Create rejected by strategy"))
      .whenever(rules).validateCreateRequest(request, today)

    assertThatThrownBy { payStatusPeriodService.create(request) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Create rejected by strategy")

    verify(rules).validateCreateRequest(request, today)
    verify(repository, never()).save(any())
  }

  @Test
  fun `should save a pay status period with a start date in the future`() {
    val request = createPayStatusPeriodRequest(
      startDate = LocalDate.now(clock).plusDays(1),
      endDate = LocalDate.now(clock).plusMonths(1),
    )

    val savedEntity = payStatusPeriod(
      startDate = request.startDate,
      endDate = request.endDate,
    )

    whenever(repository.save(any<PayStatusPeriod>())).thenReturn(savedEntity)

    val result = payStatusPeriodService.create(request)

    assertThat(result).isEqualTo(savedEntity.toModel())
    verify(rules).validateCreateRequest(request, today)
    verify(repository).save(any())
  }

  @Test
  fun `should retrieve pay status periods`() {
    val expectedEntities = listOf(
      payStatusPeriod(
        id = UUID.randomUUID(),
        prisonerNumber = "A1111AA",
        startDate = LocalDate.of(2025, 7, 23),
        endDate = LocalDate.of(2025, 11, 1),
        createdBy = "BLOGGSJ",
        createdDateTime = LocalDateTime.now(clock),
      ),
      payStatusPeriod(
        id = UUID.randomUUID(),
        prisonerNumber = "B2222BB",
        startDate = LocalDate.of(2025, 4, 13),
        createdBy = "SMITHK",
        createdDateTime = LocalDateTime.now(clock),
      ),
    )

    whenever(repository.search(any(), any(), any(), any())).thenReturn(expectedEntities)

    val results = payStatusPeriodService.search(LocalDate.of(2025, 7, 23), true, "RSI")

    verify(repository).search(LocalDate.of(2025, 7, 23), true, "RSI", LocalDate.of(2025, 7, 23))

    assertThat(results).usingRecursiveComparison().isEqualTo(expectedEntities.map { it.toModel() })
  }

  @Test
  fun `should update a pay status period`() {
    val id = UUID1

    val request = UpdatePayStatusPeriodRequest(
      endDate = today().plusDays(10),
    )

    val entity = payStatusPeriod()

    whenever(updateService.update(id, request)).thenReturn(entity.toModel())

    val result = payStatusPeriodService.update(id, request)

    assertThat(result).isEqualTo(entity.toModel())
  }

  @Test
  fun `should delete a future pay status period`() {
    val futurePayStatusPeriod = payStatusPeriod(
      startDate = LocalDate.now(clock).plusDays(10),
      endDate = LocalDate.now(clock).plusMonths(1),
    )

    whenever(repository.findById(UUID1)).thenReturn(Optional.of(futurePayStatusPeriod))

    payStatusPeriodService.delete(UUID1)

    inOrder(repository, rulesRegistry, rules) {
      verify(repository).findById(UUID1)
      verify(rulesRegistry).rulesFor(futurePayStatusPeriod.type)
      verify(rules).validateDeleteRequest(futurePayStatusPeriod, today)
      verify(repository).delete(futurePayStatusPeriod)
    }
  }

  @Test
  fun `should not delete when the selected strategy rejects delete`() {
    val existing = payStatusPeriod(startDate = LocalDate.now(clock))

    whenever(repository.findById(UUID1)).thenReturn(Optional.of(existing))
    doThrow(IllegalArgumentException("Delete rejected by strategy"))
      .whenever(rules).validateDeleteRequest(existing, today)

    assertThatThrownBy { payStatusPeriodService.delete(UUID1) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Delete rejected by strategy")

    verify(rules).validateDeleteRequest(existing, today)
    verify(repository, never()).delete(any())
  }

  @Test
  fun `should throw an exception if the pay status period does not exist`() {
    whenever(repository.findById(UUID1)).thenReturn(Optional.empty())

    assertThatThrownBy { payStatusPeriodService.delete(UUID1) }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Pay Status Period $UUID1 not found")
  }
}
