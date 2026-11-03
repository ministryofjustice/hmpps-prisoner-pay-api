package uk.gov.justice.digital.hmpps.prisonerpayapi.service.paystatusperiod.rules

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusType
import uk.gov.justice.digital.hmpps.prisonerpayapi.service.paystatusperiod.BasePayStatusPeriodRules

private const val MAX_DURATION_DAYS: Long = 28

@Component
class ShortTermSickPayStatusPeriodRules : BasePayStatusPeriodRules(MAX_DURATION_DAYS) {
  override fun payType() = PayStatusType.SHORT_TERM_SICK
}
