package uk.gov.justice.digital.hmpps.prisonerpayapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request.UpdatePayStatusPeriodRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.UUID1
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.createPayStatusPeriodRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.payStatusPeriod
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.today
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusPeriod
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.repository.PayStatusPeriodRepository
import uk.gov.justice.digital.hmpps.prisonerpayapi.mapping.toModel
import uk.gov.justice.hmpps.kotlin.auth.HmppsAuthenticationHolder
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import kotlin.jvm.java

class PayStatusPeriodServiceTest {
  val authenticationHolder: HmppsAuthenticationHolder = mock()
  val repository: PayStatusPeriodRepository = mock()
  val updateService: PayStatusPeriodUpdateService = mock()
  val clock: Clock = Clock.fixed(Instant.parse("2025-07-23T12:34:56Z"), ZoneId.of("Europe/London"))

  val payStatusPeriodService = PayStatusPeriodService(authenticationHolder, repository, updateService, clock)

  val captor = argumentCaptor<PayStatusPeriod>()

  @BeforeEach
  fun setUp() {
    whenever(authenticationHolder.username).thenReturn("BLOGGSJ")
  }

  @Test
  fun `should save pay status period`() {
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

    verify(repository).save(captor.capture())

    assertThat(captor.firstValue).usingRecursiveComparison().isEqualTo(newEntity)
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

    whenever(repository.search(any(), any(), any())).thenReturn(expectedEntities)

    val results = payStatusPeriodService.search(LocalDate.of(2025, 7, 23), true, "RSI")

    verify(repository).search(LocalDate.of(2025, 7, 23), true, "RSI")

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
  fun `should delete a pay status period when start date is in future`() {
    val futurePayStatusPeriod = payStatusPeriod(
      startDate = LocalDate.now(clock).plusDays(10),
      endDate = LocalDate.now(clock).plusMonths(1),
    )

    whenever(repository.findById(UUID1)).thenReturn(Optional.of(futurePayStatusPeriod))

    payStatusPeriodService.delete(UUID1)

    verify(repository).findById(UUID1)
    verify(repository).delete(futurePayStatusPeriod)
  }

  @Test
  fun `should throw an exception if the pay status period start date is today or in the past`() {
    val payStatusPeriod = payStatusPeriod(
      startDate = LocalDate.now(clock),
      endDate = LocalDate.now(clock).plusMonths(1),
    )

    whenever(repository.findById(UUID1)).thenReturn(Optional.of(payStatusPeriod))

    assertThatThrownBy { payStatusPeriodService.delete(UUID1) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Cannot delete a pay status period with a start date in the past or today")
  }

  @Test
  fun `should throw an exception if the pay status period does not exist`() {
    whenever(repository.findById(UUID1)).thenReturn(Optional.empty())

    assertThatThrownBy { payStatusPeriodService.delete(UUID1) }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Pay Status Period $UUID1 not found")
  }
}
