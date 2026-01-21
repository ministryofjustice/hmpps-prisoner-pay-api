package uk.gov.justice.digital.hmpps.prisonerpayapi.job

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prisonerpayapi.job.JobType.MAKE_PAYMENTS
import java.time.LocalDateTime
import java.util.*

@Service
class JobsSqsListener(
  private val makePaymentsJobHandler: MakePaymentsJobHandler,
  private val mapper: ObjectMapper,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @SqsListener("prisonerpayjobs", factory = "hmppsQueueContainerFactoryProxy", maxMessagesPerPoll = "6", maxConcurrentMessages = "6")
  internal fun onMessage(rawMessage: String) {
    log.debug("Received raw job event message $rawMessage")

    val sqsMessage = mapper.readValue(rawMessage, SQSMessage::class.java)

    when (sqsMessage.eventType) {
      MAKE_PAYMENTS -> {
        makePaymentsJobHandler.handleEvent(sqsMessage.jobBatchId, sqsMessage.jobStartDateTime, toPrisonCode(sqsMessage))
      }
    }
  }

  data class SQSMessage(
    val jobBatchId: UUID,
    val jobStartDateTime: LocalDateTime,
    val eventType: JobType,
    val messageAttributes: Map<String, Any?>,
  )

  private fun toPrisonCode(sqsMessage: SQSMessage) = mapper.convertValue(sqsMessage.messageAttributes, PrisonCodeJobEvent::class.java).prisonCode
}

interface JobEvent

data class PrisonCodeJobEvent(val prisonCode: String) : JobEvent
