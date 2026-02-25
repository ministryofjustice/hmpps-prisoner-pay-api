package uk.gov.justice.digital.hmpps.prisonerpayapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request.UpdatePayRateRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.response.PayRateDto
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.RISLEY_PRISON_CODE
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.UUID1
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.updatePayRateRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusType
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.repository.PayRateRepository
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class PayRateIntegrationTest : IntegrationTestBase() {

  private lateinit var today: LocalDate

  @Autowired
  lateinit var payRateRepository: PayRateRepository

  @BeforeEach
  fun clockSetup() {
    whenever(clock.instant()).thenReturn(Instant.parse("2026-02-01T00:00:00.00Z"))
    today = LocalDate.now(clock)
  }

  @Nested
  @DisplayName("Get pay rates")
  inner class GetPayRates {

    @Test
    @Sql("classpath:sql/pay-rates/pay-rates-with-start-dates-in-past-and-future.sql")
    fun `should return latest past pay rate and all future rates`() {
      getCurrentAndFuturePayRates(prisonCode = RISLEY_PRISON_CODE)
        .successList<PayRateDto>()
        .let { payRates ->
          assertThat(payRates).hasSize(3)

          payRates.forEach { payRate ->
            assertThat(payRate.prisonCode).isEqualTo("RSI")
            assertThat(payRate.type).isEqualTo(PayStatusType.LONG_TERM_SICK)
            assertThat(payRate.createdBy).isEqualTo("USER1")
          }

          val pastRates = payRates.filter { !it.startDate.isAfter(today) }
          val futureRates = payRates.filter { it.startDate.isAfter(today) }

          pastRates.single().let {
            assertThat(it.startDate).isEqualTo(LocalDate.of(2026, 1, 15))
            assertThat(it.rate).isEqualTo(100)
          }

          assertThat(futureRates.map { it.startDate }).containsExactly(
            LocalDate.of(2026, 2, 10),
            LocalDate.of(2026, 2, 20),
          )
          assertThat(futureRates.map { it.rate }).containsExactly(80, 110)
        }
    }

    @Test
    @Sql("classpath:sql/pay-rates/pay-rates-with-start-dates-only-in-future.sql")
    fun `should return only future pay rates when no past pay rates exist`() {
      getCurrentAndFuturePayRates(prisonCode = RISLEY_PRISON_CODE)
        .successList<PayRateDto>()
        .let { futureRates ->
          assertThat(futureRates).allMatch { it.startDate.isAfter(today) }

          futureRates.forEach { payRate ->
            assertThat(payRate.prisonCode).isEqualTo("RSI")
            assertThat(payRate.type).isEqualTo(PayStatusType.LONG_TERM_SICK)
            assertThat(payRate.createdBy).isEqualTo("USER2")
          }

          assertThat(futureRates.map { it.startDate }).containsExactly(
            LocalDate.of(2026, 2, 20),
            LocalDate.of(2026, 2, 26),
            LocalDate.of(2026, 3, 10),
          )

          assertThat(futureRates.map { it.rate }).containsExactly(75, 65, 99)
        }
    }

    @Test
    @Sql("classpath:sql/pay-rates/pay-rates-with-start-dates-only-in-past.sql")
    fun `should return latest past pay rate when no future pay rates exist`() {
      getCurrentAndFuturePayRates(prisonCode = RISLEY_PRISON_CODE)
        .successList<PayRateDto>()
        .single()
        .let { payRate ->
          assertThat(payRate.prisonCode).isEqualTo("RSI")
          assertThat(payRate.type).isEqualTo(PayStatusType.LONG_TERM_SICK)
          assertThat(payRate.createdBy).isEqualTo("USER1")
          assertThat(payRate.startDate).isEqualTo(LocalDate.of(2026, 1, 15))
        }
    }

    @Test
    @Sql("classpath:sql/pay-rates/pay-rates-with-start-dates-in-past-today-and-future.sql")
    fun `should return current (today’s) and future pay rates when past, current, and future pay rates exist`() {
      getCurrentAndFuturePayRates(prisonCode = RISLEY_PRISON_CODE)
        .successList<PayRateDto>()
        .let { payRates ->
          assertThat(payRates).hasSize(3)

          payRates.forEach { payRate ->
            assertThat(payRate.prisonCode).isEqualTo("RSI")
            assertThat(payRate.type).isEqualTo(PayStatusType.LONG_TERM_SICK)
            assertThat(payRate.createdBy).isEqualTo("USER1")
          }

          val pastRates = payRates.filter { !it.startDate.isAfter(today) }
          val futureRates = payRates.filter { it.startDate.isAfter(today) }

          pastRates.single().let {
            assertThat(it.startDate).isEqualTo(LocalDate.of(2026, 2, 1))
            assertThat(it.rate).isEqualTo(120)
          }

          assertThat(futureRates.map { it.startDate }).containsExactly(
            LocalDate.of(2026, 2, 10),
            LocalDate.of(2026, 2, 20),
          )
          assertThat(futureRates.map { it.rate }).containsExactly(80, 100)
        }
    }

    @Test
    fun `should return empty list when no past, current or future pay rates exist`() {
      assertThat(getCurrentAndFuturePayRates(prisonCode = RISLEY_PRISON_CODE).successList<PayRateDto>()).isEmpty()
    }

    @Test
    fun `returns unauthorized when no bearer token`() {
      getCurrentAndFuturePayRates(
        includeBearerAuth = false,
        prisonCode = RISLEY_PRISON_CODE,
      ).fail(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `returns forbidden when role is incorrect`() {
      getCurrentAndFuturePayRates(roles = listOf("ROLE_NO_PERMISSIONS"), prisonCode = RISLEY_PRISON_CODE).fail(
        HttpStatus.FORBIDDEN,
      )
    }
  }

  @Nested
  @DisplayName("Update pay rates")
  inner class UpdatePayRate {

    @Test
    @Sql("classpath:sql/pay-rates/update-pay-rates.sql")
    fun `should update pay rate when existing and request start date is today`() {
      val beforeCount = payRateRepository.count()

      val now = LocalDateTime.now(clock)
      val request = updatePayRateRequest(startDate = today, rate = 100)
      updatePayRate(UUID1, request)
        .success<PayRateDto>()
        .also {
          assertThat(it.id).isEqualTo(UUID1)
          assertThat(it.startDate).isEqualTo(request.startDate)
          assertThat(it.rate).isEqualTo(request.rate)
          assertThat(it.createdBy).isEqualTo("USER1")
          assertThat(it.createdDateTime).isEqualTo(LocalDateTime.of(2025, 1, 1, 0, 0))
          assertThat(it.updatedBy).isEqualTo(USERNAME)
          assertThat(it.updatedDateTime).isCloseTo(now, within(1, ChronoUnit.SECONDS))
        }
      assertThat(payRateRepository.count()).isEqualTo(beforeCount)
    }

    @Test
    @Sql("classpath:sql/pay-rates/update-pay-rates.sql")
    fun `should create new pay rate when existing start date is in past and request start date is in future and no future rate exists`() {
      whenever(clock.instant()).thenReturn(Instant.parse("2026-02-10T00:00:00.00Z"))
      today = LocalDate.now(clock)

      val beforeCount = payRateRepository.count()

      val request = updatePayRateRequest(startDate = today.plusDays(10), rate = 100)

      val now = LocalDateTime.now(clock)
      updatePayRate(UUID1, request)
        .success<PayRateDto>()
        .also {
          assertThat(it.id).isNotEqualTo(UUID1)
          assertThat(it.startDate).isEqualTo(request.startDate)
          assertThat(it.rate).isEqualTo(request.rate)
          assertThat(it.createdBy).isEqualTo(USERNAME)
          assertThat(it.createdDateTime).isCloseTo(now, within(1, ChronoUnit.SECONDS))
          assertThat(it.updatedBy).isNull()
          assertThat(it.updatedDateTime).isNull()
        }
      assertThat(payRateRepository.count()).isEqualTo(beforeCount + 1)
    }

    @Test
    @Sql("classpath:sql/pay-rates/update-pay-rates-future-rates.sql")
    fun `should return bad request when existing start date is in past and request start date is in future and future rate exists`() {
      whenever(clock.instant()).thenReturn(Instant.parse("2026-02-10T00:00:00.00Z"))
      today = LocalDate.now(clock)

      val request = updatePayRateRequest(startDate = today.plusDays(20), rate = 100)

      updatePayRate(UUID1, request)
        .fail(HttpStatus.BAD_REQUEST)
    }

    @Test
    @Sql("classpath:sql/pay-rates/update-pay-rates.sql")
    fun `should create new pay rate when existing start date is in past and request start date is today`() {
      whenever(clock.instant()).thenReturn(Instant.parse("2026-02-10T00:00:00.00Z"))
      today = LocalDate.now(clock)

      val beforeCount = payRateRepository.count()

      val request = updatePayRateRequest(startDate = today, rate = 100)

      val now = LocalDateTime.now(clock)
      updatePayRate(UUID1, request)
        .success<PayRateDto>()
        .also {
          assertThat(it.id).isNotEqualTo(UUID1)
          assertThat(it.startDate).isEqualTo(request.startDate)
          assertThat(it.rate).isEqualTo(request.rate)
          assertThat(it.createdBy).isEqualTo(USERNAME)
          assertThat(it.createdDateTime).isCloseTo(now, within(1, ChronoUnit.SECONDS))
          assertThat(it.updatedBy).isNull()
          assertThat(it.updatedDateTime).isNull()
        }
      assertThat(payRateRepository.count()).isEqualTo(beforeCount + 1)
    }

    @Test
    @Sql("classpath:sql/pay-rates/update-pay-rates.sql")
    fun `should create new pay rate when existing start date is today and request start date is in future and no future rate exists`() {
      val beforeCount = payRateRepository.count()

      val request = updatePayRateRequest(startDate = today.plusDays(10), rate = 100)

      val now = LocalDateTime.now(clock)
      updatePayRate(UUID1, request)
        .success<PayRateDto>()
        .also {
          assertThat(it.id).isNotEqualTo(UUID1)
          assertThat(it.startDate).isEqualTo(request.startDate)
          assertThat(it.rate).isEqualTo(request.rate)
          assertThat(it.createdBy).isEqualTo(USERNAME)
          assertThat(it.createdDateTime).isCloseTo(now, within(1, ChronoUnit.SECONDS))
          assertThat(it.updatedBy).isNull()
          assertThat(it.updatedDateTime).isNull()
        }
      assertThat(payRateRepository.count()).isEqualTo(beforeCount + 1)
    }

    @Test
    @Sql("classpath:sql/pay-rates/update-pay-rates-future-rates.sql")
    fun `should return bad request when existing start date is today and request start date is in future and future rate exists`() {
      val request = updatePayRateRequest(startDate = today.plusDays(10), rate = 100)

      updatePayRate(UUID1, request)
        .fail(HttpStatus.BAD_REQUEST)
    }

    @Test
    @Sql("classpath:sql/pay-rates/update-pay-rates.sql")
    fun `should return bad request when request start date is beyond 30 days from today`() {
      val request = updatePayRateRequest(startDate = today.plusDays(31), rate = 120)

      updatePayRate(UUID1, request)
        .fail(HttpStatus.BAD_REQUEST)
    }

    @Test
    @Sql("classpath:sql/pay-rates/update-pay-rates.sql")
    fun `should return bad request when request start date is in the past`() {
      val request = updatePayRateRequest(startDate = today.minusDays(10), rate = 120)

      updatePayRate(UUID1, request)
        .fail(HttpStatus.BAD_REQUEST)
    }

    @Test
    @Sql("classpath:sql/pay-rates/update-pay-rates-future-rates.sql")
    fun `should return bad request existing start date is in the future`() {
      val uuid = UUID.fromString("22222222-2222-2222-2222-222222222222")
      val request = updatePayRateRequest(startDate = today.plusDays(10), rate = 120)

      updatePayRate(uuid, request)
        .fail(HttpStatus.BAD_REQUEST)
    }

    @Test
    @Sql("classpath:sql/pay-rates/update-pay-rates-future-rates.sql")
    fun `should return bad request when duplicate pay rate exists for same prison, type and start date`() {
      val request = updatePayRateRequest(startDate = LocalDate.of(2026, 2, 10), rate = 100)
      updatePayRate(UUID1, request)
        .fail(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `should return unauthorized when no bearer token`() {
      updatePayRate(UUID.randomUUID(), updatePayRateRequest(), includeBearerAuth = false)
        .fail(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `should return forbidden when role is incorrect`() {
      updatePayRate(UUID.randomUUID(), updatePayRateRequest(), roles = listOf("ROLE_NO_PERMISSIONS"))
        .fail(HttpStatus.FORBIDDEN)
    }

    @Test
    fun `should return not found when pay rate id does not exist`() {
      updatePayRate(UUID.randomUUID(), updatePayRateRequest())
        .fail(HttpStatus.NOT_FOUND)
    }
  }

  private fun updatePayRate(
    id: UUID,
    request: UpdatePayRateRequest,
    username: String = USERNAME,
    roles: List<String> = listOf("ROLE_PRISONER_PAY__PRISONER_PAY_UI"),
    includeBearerAuth: Boolean = true,
  ) = webTestClient
    .put()
    .uri("/pay-rates/$id")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(if (includeBearerAuth) setAuthorisation(roles = roles) else noAuthorisation())
    .exchange()

  private fun getCurrentAndFuturePayRates(
    roles: List<String> = listOf("ROLE_PRISONER_PAY__PRISONER_PAY_ORCHESTRATOR_API"),
    includeBearerAuth: Boolean = true,
    prisonCode: String = "RSI",
  ) = webTestClient
    .get()
    .uri("/pay-rates/prison/$prisonCode")
    .accept(MediaType.APPLICATION_JSON)
    .headers(if (includeBearerAuth) setAuthorisation(roles = roles) else noAuthorisation())
    .exchange()
}
