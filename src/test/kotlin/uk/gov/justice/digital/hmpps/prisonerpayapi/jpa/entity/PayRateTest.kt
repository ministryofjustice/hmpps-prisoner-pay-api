package uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class PayRateTest {
  @Test
  fun `should create a PayRate entity with expected values`() {
    val startDate = LocalDate.of(2026, 1, 27)
    val createdDateTime = LocalDateTime.of(2026, 1, 27, 10, 0)

    val payRate = PayRate(
      prisonCode = "BCI",
      type = PayStatusType.LONG_TERM_SICK,
      startDate = LocalDate.of(2026, 1, 27),
      rate = 90,
      createdBy = "TEST_USER",
      createdDateTime = LocalDateTime.of(2026, 1, 27, 10, 0),
    )

    assertThat(payRate.prisonCode).isEqualTo("BCI")
    assertThat(payRate.type).isEqualTo(PayStatusType.LONG_TERM_SICK)
    assertThat(payRate.startDate).isEqualTo(startDate)
    assertThat(payRate.rate).isEqualTo(90)
    assertThat(payRate.createdBy).isEqualTo("TEST_USER")
    assertThat(payRate.createdDateTime).isEqualTo(createdDateTime)
  }

  @Test
  fun `id should be null for a newly created PayRate entity`() {
    val payRate = PayRate(
      prisonCode = "RSI",
      type = PayStatusType.LONG_TERM_SICK,
      startDate = LocalDate.of(2026, 1, 27),
      rate = 100,
      createdDateTime = LocalDateTime.of(2026, 1, 27, 10, 0),
      createdBy = "TEST_USER",
    )

    assertThat(payRate.id).isNull()
  }
}
