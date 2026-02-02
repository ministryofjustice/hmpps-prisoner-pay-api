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
  @DisplayName("Get long term sick pay rates")
  inner class GetLongTermSickPayRates {
    @Sql(
      scripts = ["classpath:sql/tear-down-all-data.sql"],
      executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
    )
    @Test
    @Sql("classpath:sql/pay-rates/long-term-sick-pay-rates-1.sql")
    fun `should return currently active and future long term sick pay rates`() {
      getLongTermSickPayRates().successList<PayRateDto>().let {
        assertThat(it).hasSize(5)
        assertThat(it.map { it.prisonCode }).containsExactly("BCI", "BCI", "RSI", "RSI", "RSI")
        assertThat(it.map { it.startDate }).containsExactly(
          LocalDate.of(2026, 1, 26),
          LocalDate.of(2026, 3, 10),
          LocalDate.of(2026, 1, 15),
          LocalDate.of(2026, 2, 10),
          LocalDate.of(2026, 2, 20),
        )
        assertThat(it.map { it.rate }).containsExactly(65, 99, 100, 80, 110)
        assertThat(it.map { it.type }).containsOnly(PayStatusType.LONG_TERM_SICK)
      }
    }

    @Test
    fun `should return empty list when no current or future long term sick pay rates exist`() {
      assertThat(getLongTermSickPayRates().successList<PayRateDto>()).isEmpty()
    }

    @Test
    fun `returns unauthorized when no bearer token`() {
      getLongTermSickPayRates(includeBearerAuth = false).fail(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `returns forbidden when role is incorrect`() {
      getLongTermSickPayRates(roles = listOf("ROLE_NO_PERMISSIONS")).fail(HttpStatus.FORBIDDEN)
    }
  }

  private fun getLongTermSickPayRates(
    roles: List<String> = listOf("ROLE_PRISONER_PAY__PRISONER_PAY_ORCHESTRATOR_API"),
    includeBearerAuth: Boolean = true,
  ) = webTestClient
    .get()
    .uri("/pay-rates/long-term-sick")
    .accept(MediaType.APPLICATION_JSON)
    .headers(if (includeBearerAuth) setAuthorisation(roles = roles) else noAuthorisation())
    .exchange()
}
