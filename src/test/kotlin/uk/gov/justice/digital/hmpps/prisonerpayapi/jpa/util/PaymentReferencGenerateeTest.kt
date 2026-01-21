package uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PaymentReferencGenerateeTest {
  @Test
  fun `should generate random references`() {
    val refs = mutableSetOf<String>()

    for (i in 1..5000) {
      val ref = generateReference()
      assertThat(ref).hasSize(12)
      refs.add(ref)
    }

    assertThat(refs).hasSize(5000)
  }
}
