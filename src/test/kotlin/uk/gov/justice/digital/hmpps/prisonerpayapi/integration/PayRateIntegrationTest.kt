package uk.gov.justice.digital.hmpps.prisonerpayapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.response.PayRateDto
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.RISLEY_PRISON_CODE
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusType
import java.time.Instant
import java.time.LocalDate

class PayRateIntegrationTest : IntegrationTestBase() {

  private lateinit var today: LocalDate

  @BeforeEach
  fun clockSetup() {
    whenever(clock.instant()).thenReturn(Instant.parse("2026-02-01T02:00:00.00Z"))
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
