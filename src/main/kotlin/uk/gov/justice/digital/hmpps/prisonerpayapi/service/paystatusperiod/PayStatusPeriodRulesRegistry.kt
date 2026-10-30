package uk.gov.justice.digital.hmpps.prisonerpayapi.service.paystatusperiod

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusType

@Component
class PayStatusPeriodRulesRegistry(allRules: List<PayStatusPeriodRules>) {
  private val rulesMap: Map<PayStatusType, PayStatusPeriodRules>

  init {
    val rulesByType = allRules.groupBy { it.payType() }
    val duplicateTypes = rulesByType.filterValues { it.size > 1 }.keys
    check(duplicateTypes.isEmpty()) { "Multiple pay status period rules defined for pay types: $duplicateTypes" }

    val missingTypes = PayStatusType.entries.toSet() - rulesByType.keys
    check(missingTypes.isEmpty()) { "No pay status period rules defined for pay types: $missingTypes" }

    rulesMap = rulesByType.mapValues { (_, rules) -> rules.single() }
  }

  // Used by callers to get the rules for a specific pay status type
  fun rulesFor(type: PayStatusType): PayStatusPeriodRules = rulesMap.getValue(type)
}
