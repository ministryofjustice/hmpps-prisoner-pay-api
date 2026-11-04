package uk.gov.justice.digital.hmpps.prisonerpayapi.service.payrate.rules

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusType

class ShortTermSickPayRateRulesTest {
  private val rules = ShortTermSickPayRateRules()

  @Test
  fun `should identify the STS pay type`() {
    assertThat(rules.payType()).isEqualTo(PayStatusType.SHORT_TERM_SICK)
  }
}
