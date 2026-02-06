package uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class PayRateTest {
  @Test
  fun `should create a PayRate entity with expected values`() {
    val expectedStartDate = LocalDate.of(2026, 1, 27)
    val expectedCreatedDateTime = LocalDateTime.of(2026, 1, 27, 10, 0)

    val payRate = PayRate(
      prisonCode = "RSI",
      type = PayStatusType.LONG_TERM_SICK,
      startDate = expectedStartDate,
      rate = 90,
      createdBy = "TEST_USER",
      createdDateTime = expectedCreatedDateTime,
    )

    with(payRate) {
      assertThat(prisonCode).isEqualTo("RSI")
      assertThat(type).isEqualTo(PayStatusType.LONG_TERM_SICK)
      assertThat(startDate).isEqualTo(expectedStartDate)
      assertThat(rate).isEqualTo(90)
      assertThat(createdBy).isEqualTo("TEST_USER")
      assertThat(createdDateTime).isEqualTo(expectedCreatedDateTime)
    }
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
