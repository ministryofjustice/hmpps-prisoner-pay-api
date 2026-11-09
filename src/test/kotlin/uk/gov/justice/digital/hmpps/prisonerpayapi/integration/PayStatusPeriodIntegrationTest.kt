package uk.gov.justice.digital.hmpps.prisonerpayapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request.CreatePayStatusPeriodRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request.UpdatePayStatusPeriodRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.response.PayStatusPeriod
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.createPayStatusPeriodRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.payStatusPeriod
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.today
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.updatePayStatusPeriodRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusType
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.repository.PayStatusPeriodRepository
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.Optional.ofNullable

class PayStatusPeriodIntegrationTest : IntegrationTestBase() {
  @Autowired
  private lateinit var payStatusPeriodRepository: PayStatusPeriodRepository

  private fun now(): LocalDateTime = LocalDateTime.now(ZoneId.of("Europe/London"))

  @Nested
  @DisplayName("Create a new Pay Status Period")
  inner class CreatePayStatusPeriod {

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

    @ParameterizedTest
    @EnumSource(PayStatusType::class)
    fun `can create a new pay status period without an end date`(type: PayStatusType) {
      val request = createPayStatusPeriodRequest(type = type, endDate = null)

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
    fun `can create a new pay status period with a start date in the future`() {
      val request = createPayStatusPeriodRequest(startDate = today().plusDays(10))

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

    @ParameterizedTest
    @CsvSource("0, 0", "0, 27", "0, 28", "40, 28")
    fun `can create and retrieve an STS period up to the duration limit`(startOffset: Long, duration: Long) {
      val startDate = today().plusDays(startOffset)
      val request = createPayStatusPeriodRequest(
        type = PayStatusType.SHORT_TERM_SICK,
        startDate = startDate,
        endDate = startDate.plusDays(duration),
      )

      val response = createPayStatusPeriod(request).success<PayStatusPeriod>()

      assertThat(response.type).isEqualTo(PayStatusType.SHORT_TERM_SICK)
      assertThat(response.startDate).isEqualTo(request.startDate)
      assertThat(response.endDate).isEqualTo(request.endDate)
      assertThat(getPayStatusPeriod(response.id).success<PayStatusPeriod>()).isEqualTo(response)
      assertThat(payStatusPeriodRepository.findById(response.id).orElseThrow().type).isEqualTo(PayStatusType.SHORT_TERM_SICK)
    }

    @ParameterizedTest
    @CsvSource("0", "40")
    fun `should return bad request without saving when STS exceeds 28 days`(startOffset: Long) {
      val startDate = today().plusDays(startOffset)
      val request = createPayStatusPeriodRequest(
        type = PayStatusType.SHORT_TERM_SICK,
        startDate = startDate,
        endDate = startDate.plusDays(29),
      )

      with(createPayStatusPeriod(request).badRequest()) {
        assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(userMessage).isEqualTo("Validation failure: The pay status period must not exceed 28 days")
        assertThat(developerMessage).isEqualTo("The pay status period must not exceed 28 days")
      }
      assertThat(payStatusPeriodRepository.count()).isZero()
    }

    @Test
    fun `LTS periods should remain unlimited`() {
      val request = createPayStatusPeriodRequest(endDate = today().plusYears(10))

      val response = createPayStatusPeriod(request).success<PayStatusPeriod>()

      assertThat(response.type).isEqualTo(PayStatusType.LONG_TERM_SICK)
      assertThat(response.endDate).isEqualTo(request.endDate)
    }

    @Test
    fun `can search for a future STS period`() {
      val request = createPayStatusPeriodRequest(
        type = PayStatusType.SHORT_TERM_SICK,
        startDate = today().plusDays(1),
        endDate = today().plusDays(28),
      )
      val response = createPayStatusPeriod(request).success<PayStatusPeriod>()

      assertThat(searchPayStatusPeriods(request.startDate).successList<PayStatusPeriod>()).containsExactly(response)
    }

    @Test
    fun `should return bad request when the start date is after end date`() {
      with(
        createPayStatusPeriod(
          createPayStatusPeriodRequest(startDate = today().plusDays(10), endDate = today().plusDays(5)),
        ).badRequest(),
      ) {
        assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(userMessage).isEqualTo("Validation failure: The end date must not be before the start date")
        assertThat(developerMessage).isEqualTo("The end date must not be before the start date")
      }
    }

    @Test
    fun `should return bad request when the end date is in the past`() {
      with(
        createPayStatusPeriod(
          createPayStatusPeriodRequest(startDate = today(), endDate = today().minusDays(1)),
        ).badRequest(),
      ) {
        assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(userMessage).isEqualTo("Validation failure: The end date must not be in the past")
        assertThat(developerMessage).isEqualTo("The end date must not be in the past")
      }
    }

    @Test
    fun `should return bad request when the start date is in the past`() {
      with(
        createPayStatusPeriod(
          createPayStatusPeriodRequest(startDate = today().minusDays(1), endDate = today().plusDays(10)),
        ).badRequest(),
      ) {
        assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(userMessage).isEqualTo("Validation failure: The start date must not be in the past")
        assertThat(developerMessage).isEqualTo("The start date must not be in the past")
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
      prisonCode = "PVI",
      prisonerNumber = "F6666FF",
      startDate = today(),
      endDate = today().plusDays(10),
    )

    @BeforeEach
    fun setUp() {
      seedPayStatusPeriod(request1)
      seedPayStatusPeriod(request2)
      seedPayStatusPeriod(request3)
      seedPayStatusPeriod(request4)
      seedPayStatusPeriod(request5)
      seedPayStatusPeriod(request6)
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
      val response = searchPayStatusPeriods(today(), prisonCode = "RSI").successList<PayStatusPeriod>()

      assertThat(response).hasSize(3)

      response.forEach {
        assertThat(it.id).isNotNull
        assertThat(it.prisonCode).isEqualTo("RSI")
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

    @ParameterizedTest
    @CsvSource(
      "LONG_TERM_SICK, false",
      "LONG_TERM_SICK, true",
      "SHORT_TERM_SICK, false",
      "SHORT_TERM_SICK, true",
    )
    fun `should reject updating expired periods without changing the stored period`(type: PayStatusType, removeEndDate: Boolean) {
      val original = seedPayStatusPeriod(
        createPayStatusPeriodRequest(type = type, startDate = today().minusDays(10), endDate = today().minusDays(1)),
      )
      val id = requireNotNull(original.id)
      val request = updatePayStatusPeriodRequest(
        endDate = if (removeEndDate) null else today().plusDays(1),
        removeEndDate = removeEndDate,
      )

      with(updatePayStatusPeriod(id, request).badRequest()) {
        assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(userMessage).isEqualTo("Validation failure: Cannot update an expired pay status period")
        assertThat(developerMessage).isEqualTo("Cannot update an expired pay status period")
      }

      assertThat(getPayStatusPeriod(id).success<PayStatusPeriod>().endDate).isEqualTo(original.endDate)
      assertThat(payStatusPeriodRepository.findById(id).orElseThrow().endDate).isEqualTo(original.endDate)
    }

    @ParameterizedTest
    @CsvSource(
      "LONG_TERM_SICK, -10, 0",
      "LONG_TERM_SICK, -10, 5",
      "LONG_TERM_SICK, 0, 5",
      "LONG_TERM_SICK, 5, 10",
      "LONG_TERM_SICK, -10,",
      "LONG_TERM_SICK, 5,",
      "SHORT_TERM_SICK, -10, 0",
      "SHORT_TERM_SICK, -10, 5",
      "SHORT_TERM_SICK, 0, 5",
      "SHORT_TERM_SICK, 5, 10",
      "SHORT_TERM_SICK, -10,",
      "SHORT_TERM_SICK, 5,",
    )
    fun `should allow end date updates for active and future periods`(type: PayStatusType, startOffset: Long, endOffset: Long?) {
      val original = seedPayStatusPeriod(
        createPayStatusPeriodRequest(
          type = type,
          startDate = today().plusDays(startOffset),
          endDate = endOffset?.let { today().plusDays(it) },
        ),
      )
      val id = requireNotNull(original.id)
      val newEndDate = today().plusDays(15)

      assertThat(updatePayStatusPeriod(id, updatePayStatusPeriodRequest(endDate = newEndDate)).success<PayStatusPeriod>().endDate)
        .isEqualTo(newEndDate)
      assertThat(payStatusPeriodRepository.findById(id).orElseThrow().endDate).isEqualTo(newEndDate)

      assertThat(
        updatePayStatusPeriod(id, updatePayStatusPeriodRequest(endDate = null, removeEndDate = true)).success<PayStatusPeriod>().endDate,
      ).isNull()
      assertThat(payStatusPeriodRepository.findById(id).orElseThrow().endDate).isNull()
    }

    @Test
    fun `should reject an over limit STS PATCH without changing the stored period`() {
      val startDate = today()
      val original = createPayStatusPeriod(
        createPayStatusPeriodRequest(type = PayStatusType.SHORT_TERM_SICK, startDate = startDate, endDate = startDate.plusDays(10)),
      ).success<PayStatusPeriod>()

      with(updatePayStatusPeriod(original.id, updatePayStatusPeriodRequest(endDate = startDate.plusDays(100))).badRequest()) {
        assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(userMessage).isEqualTo("Validation failure: The pay status period must not exceed 28 days")
        assertThat(developerMessage).isEqualTo("The pay status period must not exceed 28 days")
      }

      assertThat(getPayStatusPeriod(original.id).success<PayStatusPeriod>()).isEqualTo(original)
      assertThat(payStatusPeriodRepository.findById(original.id).orElseThrow().endDate).isEqualTo(original.endDate)
    }

    @Test
    fun `should allow an STS PATCH to the 28 day boundary`() {
      val startDate = today().plusDays(10)
      val original = createPayStatusPeriod(
        createPayStatusPeriodRequest(type = PayStatusType.SHORT_TERM_SICK, startDate = startDate, endDate = null),
      ).success<PayStatusPeriod>()
      val endDate = startDate.plusDays(28)

      assertThat(updatePayStatusPeriod(original.id, updatePayStatusPeriodRequest(endDate = endDate)).success<PayStatusPeriod>().endDate)
        .isEqualTo(endDate)
      assertThat(payStatusPeriodRepository.findById(original.id).orElseThrow().endDate).isEqualTo(endDate)
    }

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
    fun `should return bad request if the end date is in the past`() {
      with(createPayStatusPeriod(createPayStatusPeriodRequest()).success<PayStatusPeriod>()) {
        with(updatePayStatusPeriod(id, updatePayStatusPeriodRequest(endDate = today().minusDays(1))).badRequest()) {
          assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST.value())
          assertThat(userMessage).isEqualTo("Validation failure: The end date must not be in the past")
          assertThat(developerMessage).isEqualTo("The end date must not be in the past")
        }
      }
    }

    @Test
    fun `should return bad request if the end date is before the start date`() {
      with(
        createPayStatusPeriod(
          createPayStatusPeriodRequest(startDate = today().plusDays(30), endDate = today().plusDays(40)),
        ).success<PayStatusPeriod>(),
      ) {
        with(updatePayStatusPeriod(id, updatePayStatusPeriodRequest(endDate = today().plusDays(10))).badRequest()) {
          assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST.value())
          assertThat(userMessage).isEqualTo("Validation failure: The end date must not be before the start date")
          assertThat(developerMessage).isEqualTo("The end date must not be before the start date")
        }
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

  @Nested
  @DisplayName("Delete future pay status period")
  inner class DeleteFuturePayStatusPeriod {

    @Test
    fun `should delete a future pay status period`() {
      val futurePayStatusPeriod = createPayStatusPeriod(
        createPayStatusPeriodRequest(startDate = today().plusDays(10), endDate = today().plusDays(20)),
      ).success<PayStatusPeriod>()

      deleteFuturePayStatusPeriod(futurePayStatusPeriod.id).expectStatus().isNoContent

      getPayStatusPeriod(futurePayStatusPeriod.id).fail(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `should return bad request when the start date is today`() {
      val futurePayStatusPeriod = createPayStatusPeriod(
        createPayStatusPeriodRequest(startDate = today(), endDate = today().plusDays(10)),
      ).success<PayStatusPeriod>()

      with(deleteFuturePayStatusPeriod(futurePayStatusPeriod.id).badRequest()) {
        assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(userMessage).isEqualTo("Validation failure: Cannot delete a pay status period with a start date in the past or today")
        assertThat(developerMessage).isEqualTo("Cannot delete a pay status period with a start date in the past or today")
      }

      getPayStatusPeriod(futurePayStatusPeriod.id).success<PayStatusPeriod>()
    }

    @Test
    fun `should return bad request when the start date is in the past`() {
      val futurePayStatusPeriod = seedPayStatusPeriod(
        createPayStatusPeriodRequest(startDate = today().minusDays(10), endDate = today().plusDays(10)),
      )

      with(deleteFuturePayStatusPeriod(futurePayStatusPeriod.id!!).badRequest()) {
        assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(userMessage).isEqualTo("Validation failure: Cannot delete a pay status period with a start date in the past or today")
        assertThat(developerMessage).isEqualTo("Cannot delete a pay status period with a start date in the past or today")
      }

      getPayStatusPeriod(futurePayStatusPeriod.id!!).success<PayStatusPeriod>()
    }

    @Test
    fun `should return not found if id does not exist`() {
      deleteFuturePayStatusPeriod(UUID.randomUUID()).fail(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `returns unauthorized when no bearer token`() {
      deleteFuturePayStatusPeriod(UUID.randomUUID(), includeBearerAuth = false).fail(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `returns forbidden when role is incorrect`() {
      deleteFuturePayStatusPeriod(UUID.randomUUID(), roles = listOf("ROLE_NO_PERMISSIONS")).fail(HttpStatus.FORBIDDEN)
    }
  }

  @Nested
  @DisplayName("Delete a future short term sick pay status period")
  inner class DeleteShortTermSickPayStatusPeriod {
    private val fixedToday = LocalDate.of(2026, 1, 15)

    @BeforeEach
    fun fixClock() {
      whenever(clock.instant()).thenReturn(Instant.parse("2026-01-15T12:00:00Z"))
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `should delete an STS period starting tomorrow`(openEnded: Boolean) {
      val period = createPayStatusPeriod(
        createPayStatusPeriodRequest(
          type = PayStatusType.SHORT_TERM_SICK,
          startDate = fixedToday.plusDays(1),
          endDate = if (openEnded) null else fixedToday.plusDays(10),
        ),
      ).success<PayStatusPeriod>()

      deleteFuturePayStatusPeriod(period.id).expectStatus().isNoContent

      getPayStatusPeriod(period.id).fail(HttpStatus.NOT_FOUND)
      assertThat(payStatusPeriodRepository.findById(period.id)).isEmpty()
    }

    @ParameterizedTest
    @CsvSource("-1, false", "-1, true", "0, false", "0, true")
    fun `should reject deleting STS starting yesterday or today without changing the stored period`(startOffset: Long, openEnded: Boolean) {
      val period = seedPayStatusPeriod(
        createPayStatusPeriodRequest(
          type = PayStatusType.SHORT_TERM_SICK,
          startDate = fixedToday.plusDays(startOffset),
          endDate = if (openEnded) null else fixedToday.plusDays(10),
        ),
      )
      val id = requireNotNull(period.id)
      val original = getPayStatusPeriod(id).success<PayStatusPeriod>()

      with(deleteFuturePayStatusPeriod(id).badRequest()) {
        assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST.value())
        assertThat(userMessage).isEqualTo("Validation failure: Cannot delete a pay status period with a start date in the past or today")
        assertThat(developerMessage).isEqualTo("Cannot delete a pay status period with a start date in the past or today")
      }

      assertThat(getPayStatusPeriod(id).success<PayStatusPeriod>()).isEqualTo(original)
      assertThat(payStatusPeriodRepository.findById(id).orElseThrow())
        .usingRecursiveComparison().isEqualTo(period)
    }
  }

  private fun seedPayStatusPeriod(request: CreatePayStatusPeriodRequest) = payStatusPeriodRepository.save(
    payStatusPeriod(
      id = null,
      prisonCode = request.prisonCode,
      prisonerNumber = request.prisonerNumber,
      type = request.type,
      startDate = request.startDate,
      endDate = request.endDate,
      createdBy = USERNAME,
      createdDateTime = now(),
    ),
  )

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

  private fun deleteFuturePayStatusPeriod(
    id: UUID,
    username: String = USERNAME,
    roles: List<String> = listOf("ROLE_PRISONER_PAY__PRISONER_PAY_UI"),
    includeBearerAuth: Boolean = true,
  ) = webTestClient
    .delete()
    .uri("/pay-status-periods/$id")
    .accept(MediaType.APPLICATION_JSON)
    .headers(if (includeBearerAuth) setAuthorisation(roles = roles) else noAuthorisation())
    .exchange()
}
