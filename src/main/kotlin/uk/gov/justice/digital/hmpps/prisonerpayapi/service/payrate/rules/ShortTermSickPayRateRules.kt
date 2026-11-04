package uk.gov.justice.digital.hmpps.prisonerpayapi.service.payrate.rules

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusType
import uk.gov.justice.digital.hmpps.prisonerpayapi.service.payrate.BasePayRateRules

@Component
class ShortTermSickPayRateRules : BasePayRateRules() {
  override fun payType() = PayStatusType.SHORT_TERM_SICK
}
