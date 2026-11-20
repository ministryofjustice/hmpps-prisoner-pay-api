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
    private const val MAX_SEND_ATTEMPTS = 3
  }

  override fun jobType() = JobType.MAKE_PAYMENTS

  override fun execute() {
    // TODO: Will change to determine which prisons to make payments for
    val prisons = listOf("BCI", "RSI")

    log.info("Sending make payments events for ${prisons.count()} prisons")

    val batchId = UUID.randomUUID()
    val batchStartDateTime = LocalDateTime.now(clock)

    val events = prisons.map { prison ->
      JobEventMessage(
        jobBatchId = batchId,
        jobStartDateTime = batchStartDateTime,
        eventType = JobType.MAKE_PAYMENTS,
        messageAttributes = PrisonCodeJobEvent(prison),
      )
    }

    // Attempt every event, then retry only the ones SQS did not accept. Events already accepted are never resent.
    var pending = events
    repeat(MAX_SEND_ATTEMPTS) { attempt ->
      if (pending.isEmpty()) return@repeat
      pending = pending.filterNot { sendQuietly(it) }
      if (pending.isNotEmpty() && attempt < MAX_SEND_ATTEMPTS - 1) {
        log.warn("${pending.size} of ${events.size} make payments events not accepted, retrying")
      }
    }

    if (pending.isNotEmpty()) {
      val failed = pending.map { (it.messageAttributes as PrisonCodeJobEvent).prisonCode }
      throw PublishEventException("Failed to send make payments events for prisons $failed (batch $batchId); ${events.size - pending.size} of ${events.size} accepted")
    }
  }

  private fun sendQuietly(event: JobEventMessage) = try {
    jobsSqsService.sendJobEvent(event)
    true
  } catch (e: PublishEventException) {
    false
  }

  fun handleEvent(jobBatchId: UUID, jobStartDateTime: LocalDateTime, prisonCode: String) {
    log.debug("Handling event for make payments for {}, {}, {}", jobBatchId, jobStartDateTime, prisonCode)

    paymentService.processPayments(prisonCode)
  }
}
