package uk.gov.justice.digital.hmpps.prisonerpayapi.job

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.UUID1
import uk.gov.justice.digital.hmpps.prisonerpayapi.service.payment.PaymentService
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class MakePaymentsJobHandlerTest {
  val jobsSqsService: JobsSqsService = mock()
  val paymentService: PaymentService = mock()
  val clock: Clock = Clock.fixed(Instant.parse("2025-07-23T12:34:56Z"), ZoneId.of("Europe/London"))

  val handler = MakePaymentsJobHandler(jobsSqsService, paymentService, clock)

  val captor = argumentCaptor<JobEventMessage>()

  @Test
  fun `should return the job type`() {
    assert(handler.jobType() == JobType.MAKE_PAYMENTS)
  }

  @Test
  fun `should execute`() {
    handler.execute()

    verify(jobsSqsService, times(2)).sendJobEvent(captor.capture())

    val allValues = captor.allValues

    assertThat(allValues.map { it.messageAttributes }).containsExactly(PrisonCodeJobEvent("BCI"), PrisonCodeJobEvent("RSI"))

    val expectedStartTime = LocalDateTime.now(clock)

    allValues.forEach {
      assertThat(it.jobBatchId).isNotNull()
      assertThat(it.jobStartDateTime).isEqualTo(expectedStartTime)
      assertThat(it.eventType).isEqualTo(JobType.MAKE_PAYMENTS)
    }
  }

  @Test
  fun `should handle incoming message`() {
    handler.handleEvent(UUID1, LocalDateTime.now(), "BCI")

    verify(paymentService).processPayments("BCI")
  }
}
