package uk.gov.justice.digital.hmpps.prisonerpayapi.job

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prisonerpayapi.service.payment.PaymentService
import java.time.Clock
import java.time.LocalDateTime
import java.util.*

@Service
class MakePaymentsJobHandler(
  private val jobsSqsService: JobsSqsService,
  private val paymentService: PaymentService,
  private val clock: Clock,
) : JobHandler {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun jobType() = JobType.MAKE_PAYMENTS

  override fun execute() {
    // TODO: Will change to determine which prisons to make payments for
    val prisons = listOf("BCI", "RSI")

    log.info("Sending make payments events for ${prisons.count()} prisons")

    val batchId = UUID.randomUUID()
    val batchStartDateTime = LocalDateTime.now(clock)

    prisons.forEach { prison ->
      val event = JobEventMessage(
        jobBatchId = batchId,
        jobStartDateTime = batchStartDateTime,
        eventType = JobType.MAKE_PAYMENTS,
        messageAttributes = PrisonCodeJobEvent(prison),
      )

      jobsSqsService.sendJobEvent(event)
    }
  }

  fun handleEvent(jobBatchId: UUID, jobStartDateTime: LocalDateTime, prisonCode: String) {
    log.debug("Handling event for make payments for {}, {}, {}", jobBatchId, jobStartDateTime, prisonCode)

    paymentService.processPayments(prisonCode)
  }
}
