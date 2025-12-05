package uk.gov.justice.digital.hmpps.prisonerpayapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request.CreatePayStatusPeriodRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.response.PayStatusPeriod
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class PayStatusPeriodIntegrationTest : IntegrationTestBase() {
  @Test
  fun `can create a new pay status period with an end date`() {
    val request = CreatePayStatusPeriodRequest(
      prisonerNumber = "A1234AA",
      type = PayStatusType.LONG_TERM_SICK,
      startDate = LocalDate.now(),
      endDate = LocalDate.now().plusDays(10),
    )

    val response = createPayStatusPeriod(request).success<PayStatusPeriod>(HttpStatus.OK)

    with(response) {
      assertThat(id).isNotNull()
      assertThat(prisonerNumber).isEqualTo(request.prisonerNumber)
      assertThat(type).isEqualTo(request.type)
      assertThat(startDate).isEqualTo(request.startDate)
      assertThat(endDate).isEqualTo(request.endDate)
      assertThat(createdBy).isEqualTo(USERNAME)
      assertThat(createdDateTime).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.SECONDS))
    }
  }

  @Test
  fun `can create a new pay status period without an end date`() {
    val request = CreatePayStatusPeriodRequest(
      prisonerNumber = "A1234AA",
      type = PayStatusType.LONG_TERM_SICK,
      startDate = LocalDate.now(),
    )

    val response = createPayStatusPeriod(request).success<PayStatusPeriod>(HttpStatus.OK)

    with(response) {
      assertThat(id).isNotNull()
      assertThat(prisonerNumber).isEqualTo(request.prisonerNumber)
      assertThat(type).isEqualTo(request.type)
      assertThat(startDate).isEqualTo(request.startDate)
      assertThat(endDate).isNull()
      assertThat(createdBy).isEqualTo(USERNAME)
      assertThat(createdDateTime).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.SECONDS))
    }
  }

  @Test
  fun `return forbidden when no bearer token`() {
    val request = CreatePayStatusPeriodRequest(
      prisonerNumber = "A1234AA",
      type = PayStatusType.LONG_TERM_SICK,
      startDate = LocalDate.now(),
      endDate = LocalDate.now().plusDays(10),
    )

    createPayStatusPeriod(request, includeBearerAuth = false).fail(HttpStatus.UNAUTHORIZED)
  }

  private fun createPayStatusPeriod(
    request: CreatePayStatusPeriodRequest,
    username: String = USERNAME,
    roles: List<String> = listOf(),
    includeBearerAuth: Boolean = true,
  ) = webTestClient
    .post()
    .uri("/pay-status-period")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(if (includeBearerAuth) setAuthorisation() else noAuthorisation())
    .exchange()
}
