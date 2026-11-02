package uk.gov.justice.digital.hmpps.prisonerpayapi.service.paystatusperiod.rules

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusType
import uk.gov.justice.digital.hmpps.prisonerpayapi.service.paystatusperiod.BasePayStatusPeriodRules

@Component
class ShortTermSickPayStatusPeriodRules : BasePayStatusPeriodRules(28) {
  override fun payType() = PayStatusType.SHORT_TERM_SICK
}
