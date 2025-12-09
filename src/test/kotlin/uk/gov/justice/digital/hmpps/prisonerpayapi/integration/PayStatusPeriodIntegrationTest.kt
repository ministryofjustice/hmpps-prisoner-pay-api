package uk.gov.justice.digital.hmpps.prisonerpayapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
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
  @Nested
  inner class CreatePayStatusPeriod {
    @Test
    fun `can create a new pay status period with an end date`() {
      val request = CreatePayStatusPeriodRequest(
        prisonerNumber = "A1234AA",
        type = PayStatusType.LONG_TERM_SICK,
        startDate = LocalDate.now(),
        endDate = LocalDate.now().plusDays(10),
      )

      val response = createPayStatusPeriod(request).success<PayStatusPeriod>()

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

      val response = createPayStatusPeriod(request).success<PayStatusPeriod>()

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
    fun `returns forbidden when no bearer token`() {
      val request = CreatePayStatusPeriodRequest(
        prisonerNumber = "A1234AA",
        type = PayStatusType.LONG_TERM_SICK,
        startDate = LocalDate.now(),
        endDate = LocalDate.now().plusDays(10),
      )

      createPayStatusPeriod(request, includeBearerAuth = false).fail(HttpStatus.UNAUTHORIZED)
    }
  }

  @Nested
  inner class SearchPayStatusPeriods {
    val request1 = CreatePayStatusPeriodRequest(
      prisonerNumber = "A1111AA",
      type = PayStatusType.LONG_TERM_SICK,
      startDate = LocalDate.now(),
      endDate = LocalDate.now().plusDays(10),
    )

    val request2 = CreatePayStatusPeriodRequest(
      prisonerNumber = "B2222BB",
      type = PayStatusType.LONG_TERM_SICK,
      startDate = LocalDate.now().minusDays(1),
      endDate = LocalDate.now(),
    )

    val request3 = CreatePayStatusPeriodRequest(
      prisonerNumber = "C3333CC",
      type = PayStatusType.LONG_TERM_SICK,
      startDate = LocalDate.now().minusDays(10),
      endDate = LocalDate.now().minusDays(1),
    )

    val request4 = CreatePayStatusPeriodRequest(
      prisonerNumber = "D4444DD",
      type = PayStatusType.LONG_TERM_SICK,
      startDate = LocalDate.now().minusDays(1),
      endDate = LocalDate.now().plusDays(10),
    )

    val request5 = CreatePayStatusPeriodRequest(
      prisonerNumber = "E5555EE",
      type = PayStatusType.LONG_TERM_SICK,
      startDate = LocalDate.now().plusDays(1),
      endDate = LocalDate.now().plusDays(10),
    )

    @BeforeEach
    fun setUp() {
      createPayStatusPeriod(request1).success<PayStatusPeriod>()
      createPayStatusPeriod(request2).success<PayStatusPeriod>()
      createPayStatusPeriod(request3).success<PayStatusPeriod>()
      createPayStatusPeriod(request4).success<PayStatusPeriod>()
      createPayStatusPeriod(request5).success<PayStatusPeriod>()
    }

    @Test
    fun `should return active pay status periods`() {
      val response = searchPayStatusPeriods(LocalDate.now()).successList<PayStatusPeriod>()

      assertThat(response).hasSize(3)

      response.forEach {
        assertThat(it.id).isNotNull
        assertThat(it.type).isEqualTo(PayStatusType.LONG_TERM_SICK)
        assertThat(it.createdBy).isEqualTo(USERNAME)
        assertThat(it.createdDateTime).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.SECONDS))
      }

      assertThat(response).extracting("prisonerNumber", "startDate", "endDate").containsExactly(
        tuple(request2.prisonerNumber, request2.startDate, request2.endDate),
        tuple(request4.prisonerNumber, request4.startDate, request4.endDate),
        tuple(request1.prisonerNumber, request1.startDate, request1.endDate),
      )
    }

    @Test
    fun `should return all pay status periods`() {
      val response = searchPayStatusPeriods(LocalDate.now(), false).successList<PayStatusPeriod>()

      assertThat(response).hasSize(4)

      response.forEach {
        assertThat(it.id).isNotNull
        assertThat(it.type).isEqualTo(PayStatusType.LONG_TERM_SICK)
        assertThat(it.createdBy).isEqualTo(USERNAME)
        assertThat(it.createdDateTime).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.SECONDS))
      }

      assertThat(response).extracting("prisonerNumber", "startDate", "endDate").containsExactly(
        tuple(request3.prisonerNumber, request3.startDate, request3.endDate),
        tuple(request2.prisonerNumber, request2.startDate, request2.endDate),
        tuple(request4.prisonerNumber, request4.startDate, request4.endDate),
        tuple(request1.prisonerNumber, request1.startDate, request1.endDate),
      )
    }

    @Test
    fun `returns forbidden when no bearer token`() {
      CreatePayStatusPeriodRequest(
        prisonerNumber = "A1234AA",
        type = PayStatusType.LONG_TERM_SICK,
        startDate = LocalDate.now(),
        endDate = LocalDate.now().plusDays(10),
      )

      searchPayStatusPeriods(LocalDate.now(), includeBearerAuth = false).fail(HttpStatus.UNAUTHORIZED)
    }
  }

  private fun createPayStatusPeriod(
    request: CreatePayStatusPeriodRequest,
    username: String = USERNAME,
    roles: List<String> = listOf(),
    includeBearerAuth: Boolean = true,
  ) = webTestClient
    .post()
    .uri("/pay-status-periods")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(if (includeBearerAuth) setAuthorisation() else noAuthorisation())
    .exchange()

  private fun searchPayStatusPeriods(
    latestStartDate: LocalDate,
    activeOnly: Boolean = true,
    username: String = USERNAME,
    roles: List<String> = listOf(),
    includeBearerAuth: Boolean = true,
  ) = webTestClient
    .get()
    .uri { uriBuilder ->
      uriBuilder
        .path("/pay-status-periods")
        .queryParam("latestStartDate", latestStartDate)
        .queryParam("activeOnly", activeOnly)
        .build()
    }
    .accept(MediaType.APPLICATION_JSON)
    .headers(if (includeBearerAuth) setAuthorisation() else noAuthorisation())
    .exchange()
}
