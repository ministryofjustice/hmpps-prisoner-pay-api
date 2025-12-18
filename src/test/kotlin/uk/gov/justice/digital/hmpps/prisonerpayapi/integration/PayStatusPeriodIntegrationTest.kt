package uk.gov.justice.digital.hmpps.prisonerpayapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request.CreatePayStatusPeriodRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request.UpdatePayStatusPeriodRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.response.PayStatusPeriod
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.createPayStatusPeriodRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.today
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.updatePayStatusPeriodRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusType
import java.time.LocalDate
import java.time.LocalDateTime.now
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.Optional.ofNullable

class PayStatusPeriodIntegrationTest : IntegrationTestBase() {
  @Nested
  @DisplayName("Create a new Pay Status Period")
  inner class CreatePayStatusPeriod {

    @Nested
    @DisplayName("Retrieve a Pay Status Period")
    inner class RetrievePayStatusPeriod {

      @Test
      fun `should retrieve the pay status period`() {
        createPayStatusPeriod(createPayStatusPeriodRequest()).success<PayStatusPeriod>()
          .let { originalPayStatusPeriod ->
            with(getPayStatusPeriod(originalPayStatusPeriod.id).success<PayStatusPeriod>()) {
              assertThat(id).isEqualTo(originalPayStatusPeriod.id)
              assertThat(type).isEqualTo(originalPayStatusPeriod.type)
              assertThat(prisonerNumber).isEqualTo(originalPayStatusPeriod.prisonerNumber)
              assertThat(startDate).isEqualTo(originalPayStatusPeriod.startDate)
              assertThat(endDate).isEqualTo(originalPayStatusPeriod.endDate)
              assertThat(createdBy).isEqualTo(USERNAME)
              assertThat(createdDateTime).isCloseTo(now(), within(1, ChronoUnit.SECONDS))
            }
          }
      }

      @Test
      fun `should return not found if id does not exist`() {
        getPayStatusPeriod(UUID.randomUUID()).fail(HttpStatus.NOT_FOUND)
      }

      @Test
      fun `returns unauthorized when no bearer token`() {
        getPayStatusPeriod(UUID.randomUUID(), includeBearerAuth = false).fail(HttpStatus.UNAUTHORIZED)
      }

      @Test
      fun `returns forbidden when role is incorrect`() {
        getPayStatusPeriod(UUID.randomUUID(), roles = listOf("ROLE_NO_PERMISSIONS")).fail(HttpStatus.FORBIDDEN)
      }
    }

    @Test
    fun `can create a new pay status period with an end date`() {
      val request = createPayStatusPeriodRequest()

      val response = createPayStatusPeriod(request).success<PayStatusPeriod>()

      with(response) {
        assertThat(id).isNotNull()
        assertThat(prisonerNumber).isEqualTo(request.prisonerNumber)
        assertThat(type).isEqualTo(request.type)
        assertThat(startDate).isEqualTo(request.startDate)
        assertThat(endDate).isEqualTo(request.endDate)
        assertThat(createdBy).isEqualTo(USERNAME)
        assertThat(createdDateTime).isCloseTo(now(), within(1, ChronoUnit.SECONDS))
      }
    }

    @Test
    fun `can create a new pay status period without an end date`() {
      val request = createPayStatusPeriodRequest(endDate = null)

      val response = createPayStatusPeriod(request).success<PayStatusPeriod>()

      with(response) {
        assertThat(id).isNotNull()
        assertThat(prisonerNumber).isEqualTo(request.prisonerNumber)
        assertThat(type).isEqualTo(request.type)
        assertThat(startDate).isEqualTo(request.startDate)
        assertThat(endDate).isNull()
        assertThat(createdBy).isEqualTo(USERNAME)
        assertThat(createdDateTime).isCloseTo(now(), within(1, ChronoUnit.SECONDS))
      }
    }

    @Test
    fun `should return bad request start date is after end date`() {
      with(createPayStatusPeriod(createPayStatusPeriodRequest(endDate = today().minusDays(1))).badRequest()) {
        assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(userMessage).isEqualTo("Validation failure: endDate cannot be before startDate")
        assertThat(developerMessage).isEqualTo("endDate cannot be before startDate")
      }
    }

    @Test
    fun `returns unauthorized when no bearer token`() {
      val request = createPayStatusPeriodRequest(
        prisonerNumber = "A1234AA",
        type = PayStatusType.LONG_TERM_SICK,
        startDate = today(),
        endDate = today().plusDays(10),
      )

      createPayStatusPeriod(request, includeBearerAuth = false).fail(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `returns forbidden when role is incorrect`() {
      createPayStatusPeriod(createPayStatusPeriodRequest(), roles = listOf("ROLE_NO_PERMISSIONS")).fail(HttpStatus.FORBIDDEN)
    }
  }

  @Nested
  @DisplayName("Search for Pay Status Periods")
  inner class SearchPayStatusPeriods {
    val request1 = createPayStatusPeriodRequest(
      prisonerNumber = "A1111AA",
      startDate = today(),
      endDate = today().plusDays(10),
    )

    val request2 = createPayStatusPeriodRequest(
      prisonerNumber = "B2222BB",
      startDate = today().minusDays(1),
      endDate = today(),
    )

    val request3 = createPayStatusPeriodRequest(
      prisonerNumber = "C3333CC",
      startDate = today().minusDays(10),
      endDate = today().minusDays(1),
    )

    val request4 = createPayStatusPeriodRequest(
      prisonerNumber = "D4444DD",
      startDate = today().minusDays(1),
      endDate = today().plusDays(10),
    )

    val request5 = createPayStatusPeriodRequest(
      prisonerNumber = "E5555EE",
      startDate = today().plusDays(1),
      endDate = today().plusDays(10),
    )

    val request6 = createPayStatusPeriodRequest(
      prisonCode = "RSI",
      prisonerNumber = "F6666FF",
      startDate = today(),
      endDate = today().plusDays(10),
    )

    @BeforeEach
    fun setUp() {
      createPayStatusPeriod(request1).success<PayStatusPeriod>()
      createPayStatusPeriod(request2).success<PayStatusPeriod>()
      createPayStatusPeriod(request3).success<PayStatusPeriod>()
      createPayStatusPeriod(request4).success<PayStatusPeriod>()
      createPayStatusPeriod(request5).success<PayStatusPeriod>()
      createPayStatusPeriod(request6).success<PayStatusPeriod>()
    }

    @Test
    fun `should return active pay status periods for all prisons`() {
      val response = searchPayStatusPeriods(today()).successList<PayStatusPeriod>()

      assertThat(response).hasSize(4)

      response.forEach {
        assertThat(it.id).isNotNull
        assertThat(it.type).isEqualTo(PayStatusType.LONG_TERM_SICK)
        assertThat(it.createdBy).isEqualTo(USERNAME)
        assertThat(it.createdDateTime).isCloseTo(now(), within(1, ChronoUnit.SECONDS))
      }

      assertThat(response).extracting("prisonCode", "prisonerNumber", "startDate", "endDate").containsExactly(
        tuple(request2.prisonCode, request2.prisonerNumber, request2.startDate, request2.endDate),
        tuple(request4.prisonCode, request4.prisonerNumber, request4.startDate, request4.endDate),
        tuple(request1.prisonCode, request1.prisonerNumber, request1.startDate, request1.endDate),
        tuple(request6.prisonCode, request6.prisonerNumber, request6.startDate, request6.endDate),
      )
    }

    @Test
    fun `should return active pay status periods for a prison`() {
      val response = searchPayStatusPeriods(today(), prisonCode = "PVI").successList<PayStatusPeriod>()

      assertThat(response).hasSize(3)

      response.forEach {
        assertThat(it.id).isNotNull
        assertThat(it.prisonCode).isEqualTo("PVI")
        assertThat(it.type).isEqualTo(PayStatusType.LONG_TERM_SICK)
        assertThat(it.createdBy).isEqualTo(USERNAME)
        assertThat(it.createdDateTime).isCloseTo(now(), within(1, ChronoUnit.SECONDS))
      }

      assertThat(response).extracting("prisonerNumber", "startDate", "endDate").containsExactly(
        tuple(request2.prisonerNumber, request2.startDate, request2.endDate),
        tuple(request4.prisonerNumber, request4.startDate, request4.endDate),
        tuple(request1.prisonerNumber, request1.startDate, request1.endDate),
      )
    }

    @Test
    fun `should return all pay status periods`() {
      val response = searchPayStatusPeriods(today(), false).successList<PayStatusPeriod>()

      assertThat(response).hasSize(5)

      response.forEach {
        assertThat(it.id).isNotNull
        assertThat(it.type).isEqualTo(PayStatusType.LONG_TERM_SICK)
        assertThat(it.createdBy).isEqualTo(USERNAME)
        assertThat(it.createdDateTime).isCloseTo(now(), within(1, ChronoUnit.SECONDS))
      }

      assertThat(response).extracting("prisonerNumber", "startDate", "endDate").containsExactly(
        tuple(request3.prisonerNumber, request3.startDate, request3.endDate),
        tuple(request2.prisonerNumber, request2.startDate, request2.endDate),
        tuple(request4.prisonerNumber, request4.startDate, request4.endDate),
        tuple(request1.prisonerNumber, request1.startDate, request1.endDate),
        tuple(request6.prisonerNumber, request6.startDate, request6.endDate),
      )
    }

    @Test
    fun `returns unauthorized when no bearer token`() {
      searchPayStatusPeriods(today(), includeBearerAuth = false).fail(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `returns forbidden when role is incorrect`() {
      searchPayStatusPeriods(today(), roles = listOf("ROLE_NO_PERMISSIONS")).fail(HttpStatus.FORBIDDEN)
    }
  }

  @Nested
  @DisplayName("Update a Pay Status Period")
  inner class UpdatePayStatusPeriod {

    @Test
    fun `should update end date of the pay status period`() {
      val twoMonths = today().plusMonths(2)
      val sixMonths = twoMonths.plusMonths(4)

      with(createPayStatusPeriod(createPayStatusPeriodRequest(endDate = twoMonths)).success<PayStatusPeriod>()) {
        assertThat(endDate).isEqualTo(twoMonths)

        updatePayStatusPeriod(id, updatePayStatusPeriodRequest(endDate = sixMonths)).success<PayStatusPeriod>()
          .let { assertThat(it.endDate).isEqualTo(sixMonths) }

        getPayStatusPeriod(id).success<PayStatusPeriod>()
          .let { assertThat(it.endDate).isEqualTo(sixMonths) }
      }
    }

    @Test
    fun `should remove the end date of the pay status period`() {
      with(createPayStatusPeriod(createPayStatusPeriodRequest()).success<PayStatusPeriod>()) {
        updatePayStatusPeriod(id, updatePayStatusPeriodRequest(endDate = null, removeEndDate = true)).success<PayStatusPeriod>()
          .let { assertThat(it.endDate).isNull() }
      }
    }

    @Test
    fun `should return not found if id does not exist`() {
      updatePayStatusPeriod(UUID.randomUUID(), updatePayStatusPeriodRequest()).fail(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `should return bad request if endDate and removeEndDate are supplied`() {
      with(updatePayStatusPeriod(UUID.randomUUID(), updatePayStatusPeriodRequest(removeEndDate = true)).badRequest()) {
        assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(userMessage).isEqualTo("Validation failure: removeEndDate cannot be true when an endDate is also supplied")
        assertThat(developerMessage).isEqualTo("removeEndDate cannot be true when an endDate is also supplied")
      }
    }

    @Test
    fun `returns unauthorized when no bearer token`() {
      updatePayStatusPeriod(UUID.randomUUID(), updatePayStatusPeriodRequest(), includeBearerAuth = false).fail(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `returns forbidden when role is incorrect`() {
      updatePayStatusPeriod(UUID.randomUUID(), updatePayStatusPeriodRequest(), roles = listOf("ROLE_NO_PERMISSIONS")).fail(HttpStatus.FORBIDDEN)
    }
  }

  private fun getPayStatusPeriod(
    id: UUID,
    username: String = USERNAME,
    roles: List<String> = listOf("ROLE_PRISONER_PAY__PRISONER_PAY_ORCHESTRATOR_API"),
    includeBearerAuth: Boolean = true,
  ) = webTestClient
    .get()
    .uri("/pay-status-periods/$id")
    .accept(MediaType.APPLICATION_JSON)
    .headers(if (includeBearerAuth) setAuthorisation(roles = roles) else noAuthorisation())
    .exchange()

  private fun createPayStatusPeriod(
    request: CreatePayStatusPeriodRequest,
    username: String = USERNAME,
    roles: List<String> = listOf("ROLE_PRISONER_PAY__PRISONER_PAY_UI"),
    includeBearerAuth: Boolean = true,
  ) = webTestClient
    .post()
    .uri("/pay-status-periods")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(if (includeBearerAuth) setAuthorisation(roles = roles) else noAuthorisation())
    .exchange()

  private fun searchPayStatusPeriods(
    latestStartDate: LocalDate,
    activeOnly: Boolean = true,
    prisonCode: String? = null,
    username: String = USERNAME,
    roles: List<String> = listOf("ROLE_PRISONER_PAY__PRISONER_PAY_ORCHESTRATOR_API"),
    includeBearerAuth: Boolean = true,
  ) = webTestClient
    .get()
    .uri { uriBuilder ->
      uriBuilder
        .path("/pay-status-periods")
        .queryParam("latestStartDate", latestStartDate)
        .queryParam("activeOnly", activeOnly)
        .queryParamIfPresent("prisonCode", ofNullable(prisonCode))
        .build()
    }
    .accept(MediaType.APPLICATION_JSON)
    .headers(if (includeBearerAuth) setAuthorisation(roles = roles) else noAuthorisation())
    .exchange()

  private fun updatePayStatusPeriod(
    id: UUID,
    request: UpdatePayStatusPeriodRequest,
    username: String = USERNAME,
    roles: List<String> = listOf("ROLE_PRISONER_PAY__PRISONER_PAY_UI"),
    includeBearerAuth: Boolean = true,
  ) = webTestClient
    .patch()
    .uri("/pay-status-periods/$id")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(if (includeBearerAuth) setAuthorisation(roles = roles) else noAuthorisation())
    .exchange()
}
