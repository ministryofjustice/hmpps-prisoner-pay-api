package uk.gov.justice.digital.hmpps.prisonerpayapi.service.payrate

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusType

@Component
class PayRateRulesRegistry(allRules: List<PayRateRules>) {
  private val rulesMap: Map<PayStatusType, PayRateRules>

  init {
    val rulesByType = allRules.groupBy { it.payType() }
    val duplicateTypes = rulesByType.filterValues { it.size > 1 }.keys
    check(duplicateTypes.isEmpty()) { "Multiple pay rate rules defined for pay types: $duplicateTypes" }

    val missingTypes = PayStatusType.entries.toSet() - rulesByType.keys
    check(missingTypes.isEmpty()) { "No pay rate rules defined for pay types: $missingTypes" }

    rulesMap = rulesByType.mapValues { (_, rules) -> rules.single() }
  }

  fun rulesFor(type: PayStatusType): PayRateRules = rulesMap.getValue(type)
}
