package uk.gov.justice.digital.hmpps.prisonerpayapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.response.PayRateDto
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusType
import java.time.LocalDate

class PayRateIntegrationTest : IntegrationTestBase() {
  @Nested
  @DisplayName("Get pay rates")
  inner class GetPayRates {
    @Test
    @Sql("classpath:sql/pay-rates/pay-rates-with-start-dates-in-past-and-future.sql")
    fun `should return latest past pay rate and all future rates`() {
      getCurrentAndFuturePayRates().successList<PayRateDto>().let {
        assertThat(it).hasSize(3)
        assertThat(it.map { it.prisonCode }).containsOnly("RSI")
        assertThat(it.map { it.startDate }).containsExactly(
          LocalDate.of(2026, 1, 15),
          LocalDate.of(2026, 2, 10),
          LocalDate.of(2026, 2, 20),
        )
        assertThat(it.map { it.rate }).containsExactly(100, 80, 110)
        assertThat(it.map { it.type }).containsOnly(PayStatusType.LONG_TERM_SICK)
        assertThat(it.map { it.createdBy }).containsOnly("USER1")
      }
    }

    @Test
    @Sql("classpath:sql/pay-rates/pay-rates-with-start-dates-only-in-future.sql")
    fun `should return only future pay rates when no past pay rates exist`() {
      getCurrentAndFuturePayRates(prisonCode = PRISONCODE).successList<PayRateDto>().let {
        assertThat(it).hasSize(3)
        assertThat(it.map { it.prisonCode }).containsOnly("RSI")
        assertThat(it.map { it.startDate }).containsExactly(
          LocalDate.of(2026, 2, 20),
          LocalDate.of(2026, 2, 26),
          LocalDate.of(2026, 3, 10),
        )
        assertThat(it.map { it.rate }).containsExactly(75, 65, 99)
        assertThat(it.map { it.type }).containsOnly(PayStatusType.LONG_TERM_SICK)
        assertThat(it.map { it.createdBy }).containsOnly("USER2")
      }
    }

    @Test
    @Sql("classpath:sql/pay-rates/pay-rates-with-start-dates-only-in-past.sql")
    fun `should return latest past pay rate when no future pay rates exist`() {
      getCurrentAndFuturePayRates(prisonCode = PRISONCODE).successList<PayRateDto>().let {
        assertThat(it).hasSize(1)
        assertThat(it.map { it.prisonCode }).containsOnly("RSI")
        assertThat(it.map { it.startDate }).containsOnly(
          LocalDate.of(2026, 1, 15),
        )
        assertThat(it.map { it.rate }).containsOnly(100)
        assertThat(it.map { it.type }).containsOnly(PayStatusType.LONG_TERM_SICK)
        assertThat(it.map { it.createdBy }).containsOnly("USER1")
      }
    }

    @Test
    @Sql("classpath:sql/pay-rates/pay-rates-with-start-dates-in-past-today-and-future.sql")
    fun `should return current (today’s) and future pay rates when past, current, and future pay rates exist`() {
      getCurrentAndFuturePayRates(prisonCode = PRISONCODE).successList<PayRateDto>().let {
        assertThat(it).hasSize(2)
        assertThat(it.map { it.prisonCode }).containsOnly("RSI")
        assertThat(it.map { it.startDate }).containsExactly(
          LocalDate.now(),
          LocalDate.of(2026, 2, 10),
        )
        assertThat(it.map { it.rate }).containsExactly(100, 80)
        assertThat(it.map { it.type }).containsOnly(PayStatusType.LONG_TERM_SICK)
        assertThat(it.map { it.createdBy }).containsOnly("USER1")
      }
    }

    @Test
    fun `should return empty list when no past, current or future pay rates exist`() {
      assertThat(getCurrentAndFuturePayRates(prisonCode = PRISONCODE).successList<PayRateDto>()).isEmpty()
    }

    @Test
    fun `returns unauthorized when no bearer token`() {
      getCurrentAndFuturePayRates(includeBearerAuth = false, prisonCode = PRISONCODE).fail(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `returns forbidden when role is incorrect`() {
      getCurrentAndFuturePayRates(roles = listOf("ROLE_NO_PERMISSIONS"), prisonCode = PRISONCODE).fail(HttpStatus.FORBIDDEN)
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
