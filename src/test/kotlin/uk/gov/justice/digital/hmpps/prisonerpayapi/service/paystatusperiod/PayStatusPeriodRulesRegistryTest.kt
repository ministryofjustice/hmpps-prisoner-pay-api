package uk.gov.justice.digital.hmpps.prisonerpayapi.service.paystatusperiod

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request.CreatePayStatusPeriodRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusPeriod
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusType
import uk.gov.justice.digital.hmpps.prisonerpayapi.service.paystatusperiod.rules.LongTermSickPayStatusPeriodRules
import java.time.LocalDate

class PayStatusPeriodRulesRegistryTest {
  @Test
  fun `should return the registered LTS strategy`() {
    val rules = LongTermSickPayStatusPeriodRules()
    val registry = PayStatusPeriodRulesRegistry(listOf(rules))

    assertThat(registry.rulesFor(PayStatusType.LONG_TERM_SICK)).isSameAs(rules)
  }

  @Test
  fun `should reject missing strategies at construction`() {
    val missingType = PayStatusType.LONG_TERM_SICK
    val rulesForAllOtherTypes = PayStatusType.entries
      .filter { it != missingType }
      .map { stubRulesFor(it) }

    assertThatThrownBy { PayStatusPeriodRulesRegistry(rulesForAllOtherTypes) }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessage("No pay status period rules defined for pay types: [$missingType]")
  }

  @Test
  fun `should reject duplicate strategies instead of overwriting one`() {
    assertThatThrownBy {
      PayStatusPeriodRulesRegistry(listOf(LongTermSickPayStatusPeriodRules(), LongTermSickPayStatusPeriodRules()))
    }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessage("Multiple pay status period rules defined for pay types: [LONG_TERM_SICK]")
  }

  private fun stubRulesFor(type: PayStatusType) = object : PayStatusPeriodRules {
    override fun payType() = type
    override fun validateCreateRequest(request: CreatePayStatusPeriodRequest, today: LocalDate) {}
    override fun validateDeleteRequest(existing: PayStatusPeriod, today: LocalDate) {}
  }
}
