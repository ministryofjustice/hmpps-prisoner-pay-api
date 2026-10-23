package uk.gov.justice.digital.hmpps.prisonerpayapi.service.payrate

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request.UpdatePayRateRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayRate
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusType
import uk.gov.justice.digital.hmpps.prisonerpayapi.service.payrate.rules.LongTermSickPayRateRules
import java.time.LocalDate

class PayRateRulesRegistryTest {
  @Test
  fun `should return the registered LTS strategy`() {
    val ltsRules = LongTermSickPayRateRules()
    val registry = PayRateRulesRegistry(listOf(ltsRules))

    assertThat(registry.rulesFor(PayStatusType.LONG_TERM_SICK)).isSameAs(ltsRules)
  }

  @Test
  fun `should reject missing strategies at construction`() {
    val missingType = PayStatusType.LONG_TERM_SICK
    val rulesForAllOtherTypes = PayStatusType.entries
      .filter { it != missingType }
      .map { stubRulesFor(it) }

    assertThatThrownBy { PayRateRulesRegistry(rulesForAllOtherTypes) }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessage("No pay rate rules defined for pay types: [$missingType]")
  }

  @Test
  fun `should reject duplicate strategies instead of overwriting one`() {
    assertThatThrownBy {
      PayRateRulesRegistry(listOf(LongTermSickPayRateRules(), LongTermSickPayRateRules()))
    }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessage("Multiple pay rate rules defined for pay types: [LONG_TERM_SICK]")
  }

  private fun stubRulesFor(type: PayStatusType) = object : PayRateRules {
    override fun payType() = type
    override fun validateUpdateRequest(existing: PayRate, request: UpdatePayRateRequest, today: LocalDate) {}
    override fun validateDeleteRequest(existing: PayRate, today: LocalDate) {}
  }
}
