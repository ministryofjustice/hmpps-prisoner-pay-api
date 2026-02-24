package uk.gov.justice.digital.hmpps.prisonerpayapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request.UpdatePayStatusPeriodRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.UUID1
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.payStatusPeriod
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.today
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusPeriod
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.repository.PayStatusPeriodRepository
import java.util.*

class PayStatusPeriodUpdateServiceTest {
  val repository: PayStatusPeriodRepository = mock()

  val service = PayStatusPeriodUpdateService(repository)

  val captor = argumentCaptor<PayStatusPeriod>()

  @Test
  fun `should set end date`() {
    val newEndDate = today().plusDays(100)

    val savedEntity = payStatusPeriod()

    whenever(repository.findById(UUID1)).thenReturn(Optional.of(savedEntity))

    whenever(repository.saveAndFlush(any<PayStatusPeriod>())).thenReturn(savedEntity)

    val result = service.update(UUID1, UpdatePayStatusPeriodRequest(endDate = newEndDate))

    assertThat(result.endDate).isEqualTo(newEndDate)

    verify(repository).saveAndFlush(captor.capture())

    assertThat(captor.firstValue.endDate).isEqualTo(newEndDate)
  }

  @Test
  fun `should remove the end date`() {
    val savedEntity = payStatusPeriod()

    whenever(repository.findById(UUID1)).thenReturn(Optional.of(savedEntity))

    whenever(repository.saveAndFlush(any<PayStatusPeriod>())).thenReturn(savedEntity)

    val result = service.update(UUID1, UpdatePayStatusPeriodRequest(removeEndDate = true))

    assertThat(result.endDate).isNull()

    verify(repository).saveAndFlush(captor.capture())

    assertThat(captor.firstValue.endDate).isNull()
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
}
