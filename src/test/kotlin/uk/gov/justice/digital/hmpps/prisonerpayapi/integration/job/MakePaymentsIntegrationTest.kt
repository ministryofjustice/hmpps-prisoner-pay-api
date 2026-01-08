package uk.gov.justice.digital.hmpps.prisonerpayapi.integration.job

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.prisonerpayapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.prisonerpayapi.job.JobType

class MakePaymentsIntegrationTest : IntegrationTestBase() {
  @Test
  fun `should make payments`() {
    initiateJob(JobType.MAKE_PAYMENTS.name).expectStatus().isOk

    // TODO: Check payments were made
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
