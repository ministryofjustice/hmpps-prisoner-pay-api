package uk.gov.justice.digital.hmpps.prisonerpayapi.integration.job

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.prisonerpayapi.common.PaymentType
import uk.gov.justice.digital.hmpps.prisonerpayapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.clearQueues
import uk.gov.justice.digital.hmpps.prisonerpayapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.prisonerpayapi.job.JobType
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.repository.PaymentRepository
import uk.gov.justice.hmpps.sqs.HmppsQueue
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

class MakePaymentsIntegrationTest : IntegrationTestBase() {
  @Autowired
  lateinit var paymentRepository: PaymentRepository

  private val jobsQueue by lazy { hmppsQueueService.findByQueueId("prisonerpayjobs") as HmppsQueue }
  private val jobsClient by lazy { jobsQueue.sqsClient }

  @BeforeEach
  fun `clear job queues`() {
    clearQueues(jobsClient, jobsQueue)
  }

  @Test
  @Sql("classpath:sql/make-payments/make-payments-1.sql")
  fun `should make payments`() {
    whenever(clock.instant()).thenReturn(Instant.parse("2026-01-21T02:00:00.00Z"))

    initiateJob(JobType.MAKE_PAYMENTS.name).expectStatus().isOk

    await untilAsserted {
      val paymentsMade = paymentRepository.findAll()

      assertThat(paymentsMade).hasSize(10)

      paymentsMade.forEach { payment ->
        with(payment) {
          assertThat(prisonCode).isEqualTo("RSI")
          assertThat(prisonerNumber).isEqualTo("A1111AA")
          assertThat(paymentType).isEqualTo(PaymentType.LONG_TERM_SICK)
          assertThat(paymentDateTime).isEqualTo(LocalDateTime.now(clock))
          assertThat(reference).hasSize(12)
        }
      }

      assertThat(paymentsMade).extracting("eventDate", "timeSlot", "paymentAmount").contains(
        Tuple(LocalDate.of(2026, 1, 14), TimeSlot.AM, 50),
        Tuple(LocalDate.of(2026, 1, 14), TimeSlot.PM, 49),
        Tuple(LocalDate.of(2026, 1, 15), TimeSlot.AM, 50),
        Tuple(LocalDate.of(2026, 1, 15), TimeSlot.PM, 49),
        Tuple(LocalDate.of(2026, 1, 15), TimeSlot.AM, 50),
        Tuple(LocalDate.of(2026, 1, 16), TimeSlot.PM, 49),
        Tuple(LocalDate.of(2026, 1, 19), TimeSlot.AM, 50),
        Tuple(LocalDate.of(2026, 1, 19), TimeSlot.PM, 49),
        Tuple(LocalDate.of(2026, 1, 20), TimeSlot.AM, 50),
        Tuple(LocalDate.of(2026, 1, 20), TimeSlot.PM, 49),
      )
    }
  }

  @Test
  fun `should return error if job name is not recognised`() {
    with(initiateJob("Blah").badRequest()) {
      assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST.value())
      assertThat(userMessage).isEqualTo("Validation failure: Unrecognised job name: Blah")
      assertThat(developerMessage).isEqualTo("Unrecognised job name: Blah")
    }
  }

  private fun initiateJob(jobName: String) = webTestClient
    .post()
    .uri("/job-admin/run/$jobName")
    .accept(MediaType.APPLICATION_JSON)
    .exchange()
}
