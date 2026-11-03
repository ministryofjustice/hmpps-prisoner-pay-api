package uk.gov.justice.digital.hmpps.prisonerpayapi.service.paystatusperiod

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request.CreatePayStatusPeriodRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request.UpdatePayStatusPeriodRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusPeriod
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusType
import uk.gov.justice.digital.hmpps.prisonerpayapi.service.paystatusperiod.rules.LongTermSickPayStatusPeriodRules
import uk.gov.justice.digital.hmpps.prisonerpayapi.service.paystatusperiod.rules.ShortTermSickPayStatusPeriodRules
import java.time.LocalDate

class PayStatusPeriodRulesRegistryTest {
  @ParameterizedTest
  @EnumSource(PayStatusType::class)
  fun `should return the registered strategy for each type`(type: PayStatusType) {
    val allRules = listOf(LongTermSickPayStatusPeriodRules(), ShortTermSickPayStatusPeriodRules())
    val registry = PayStatusPeriodRulesRegistry(allRules)

    assertThat(registry.rulesFor(type)).isSameAs(allRules.single { it.payType() == type })
  }

  @ParameterizedTest
  @EnumSource(PayStatusType::class)
  fun `should reject missing strategies at construction`(missingType: PayStatusType) {
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
    override fun validateUpdateRequest(existing: PayStatusPeriod, request: UpdatePayStatusPeriodRequest) {}
    override fun validateDeleteRequest(existing: PayStatusPeriod, today: LocalDate) {}
  }
}
