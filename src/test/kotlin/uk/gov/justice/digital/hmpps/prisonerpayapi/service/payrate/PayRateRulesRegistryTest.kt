package uk.gov.justice.digital.hmpps.prisonerpayapi.service.payrate

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request.UpdatePayRateRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayRate
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusType
import uk.gov.justice.digital.hmpps.prisonerpayapi.service.payrate.rules.LongTermSickPayRateRules
import uk.gov.justice.digital.hmpps.prisonerpayapi.service.payrate.rules.ShortTermSickPayRateRules
import java.time.LocalDate

class PayRateRulesRegistryTest {
  @ParameterizedTest
  @EnumSource(PayStatusType::class)
  fun `should return the registered rules for each pay type`(type: PayStatusType) {
    val allRules = listOf(LongTermSickPayRateRules(), ShortTermSickPayRateRules())
    val registry = PayRateRulesRegistry(allRules)

    assertThat(registry.rulesFor(type)).isSameAs(allRules.single { it.payType() == type })
  }

  @ParameterizedTest
  @EnumSource(PayStatusType::class)
  fun `should reject missing strategies at construction`(missingType: PayStatusType) {
    val rulesForAllOtherTypes = PayStatusType.entries
      .filter { it != missingType }
      .map { stubRulesFor(it) }

    assertThatThrownBy { PayRateRulesRegistry(rulesForAllOtherTypes) }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessage("No pay rate rules defined for pay types: [$missingType]")
  }

  @ParameterizedTest
  @EnumSource(PayStatusType::class)
  fun `should reject duplicate strategies instead of overwriting one`(duplicateType: PayStatusType) {
    val duplicates = listOf(stubRulesFor(duplicateType), stubRulesFor(duplicateType))
    val others = PayStatusType.entries.filter { it != duplicateType }.map { stubRulesFor(it) }

    assertThatThrownBy { PayRateRulesRegistry(duplicates + others) }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessage("Multiple pay rate rules defined for pay types: [$duplicateType]")
  }

  private fun stubRulesFor(type: PayStatusType) = object : PayRateRules {
    override fun payType() = type
    override fun validateUpdateRequest(existing: PayRate, request: UpdatePayRateRequest, today: LocalDate) {}
    override fun validateDeleteRequest(existing: PayRate, today: LocalDate) {}
  }
}
