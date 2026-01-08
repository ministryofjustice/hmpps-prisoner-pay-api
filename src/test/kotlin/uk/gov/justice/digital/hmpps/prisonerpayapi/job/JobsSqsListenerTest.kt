package uk.gov.justice.digital.hmpps.prisonerpayapi.job

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.UUID1
import java.time.LocalDateTime

class JobsSqsListenerTest {
  val makePaymentsService: MakePaymentsService = mock()
  val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())
  val listener = JobsSqsListener(makePaymentsService, mapper)

  @Test
  fun `should handle MAKE_PAYMENTS event`() {
    val now = LocalDateTime.now()

    val rawMessage = """
      {
        "jobBatchId": "$UUID1",
        "jobStartDateTime": "$now",
        "eventType": "MAKE_PAYMENTS",
        "messageAttributes": {
          "prisonCode": "RSI"
        }
      }
    """

    listener.onMessage(rawMessage)

    verify(makePaymentsService).handleEvent(UUID1, now, "RSI")
  }
}
