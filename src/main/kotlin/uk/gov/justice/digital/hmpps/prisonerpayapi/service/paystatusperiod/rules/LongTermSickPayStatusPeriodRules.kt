package uk.gov.justice.digital.hmpps.prisonerpayapi.service.paystatusperiod.rules

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusType
import uk.gov.justice.digital.hmpps.prisonerpayapi.service.paystatusperiod.BasePayStatusPeriodRules

@Component
class LongTermSickPayStatusPeriodRules : BasePayStatusPeriodRules() {
  override fun payType() = PayStatusType.LONG_TERM_SICK
}
