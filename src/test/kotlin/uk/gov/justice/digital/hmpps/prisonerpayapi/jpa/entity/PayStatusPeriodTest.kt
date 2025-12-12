package uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.payStatusPeriod
import java.time.LocalDate
import java.time.LocalDateTime

class PayStatusPeriodTest {
  val today = LocalDate.now()

  @Test
  fun `should create a new entity when end date is same as start date`() {
    payStatusPeriod(endDate = today)
  }

  @Test
  fun `should create a new entity when end date is after start date`() {
    payStatusPeriod()
  }

  @Test
  fun `should create a new entity when end date is null`() {
    payStatusPeriod(endDate = null)
  }

  @Test
  fun `should fail to create a new entity when start date is after end date`() {
    assertThatThrownBy {
      payStatusPeriod(endDate = today.minusDays(1))
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("endDate cannot be before startDate")
  }

  @Test
  fun `should fail to set end date if start date is after end date`() {
    val entity = payStatusPeriod()

    assertThatThrownBy {
      entity.endDate = today.minusDays(1)
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("endDate cannot be before startDate")
  }
}
