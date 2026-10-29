package uk.gov.justice.digital.hmpps.prisonerpayapi.service.payrate.rules

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusType

class LongTermSickPayRateRulesTest {
  private val rules = LongTermSickPayRateRules()

  @Test
  fun `should identify the LTS pay type`() {
    assertThat(rules.payType()).isEqualTo(PayStatusType.LONG_TERM_SICK)
  }
}
