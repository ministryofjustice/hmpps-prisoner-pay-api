package uk.gov.justice.digital.hmpps.prisonerpayapi.service.paystatusperiod.rules

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.createPayStatusPeriodRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusType
import java.time.LocalDate

class LongTermSickPayStatusPeriodRulesTest {
  private val rules = LongTermSickPayStatusPeriodRules()
  private val today = LocalDate.of(2026, 1, 1)

  @Test
  fun `should identify the LTS pay type`() {
    assertThat(rules.payType()).isEqualTo(PayStatusType.LONG_TERM_SICK)
  }

  @Test
  fun `should apply no maximum duration for LTS`() {
    val request = createPayStatusPeriodRequest(startDate = today, endDate = today.plusYears(10))

    assertThatCode { rules.validateCreateRequest(request, today) }.doesNotThrowAnyException()
  }
}
