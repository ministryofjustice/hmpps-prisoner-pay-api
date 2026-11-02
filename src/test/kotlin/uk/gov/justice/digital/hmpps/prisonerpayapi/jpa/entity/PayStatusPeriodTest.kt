package uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import uk.gov.justice.digital.hmpps.prisonerpayapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.payStatusPeriod
import java.time.LocalDate

class PayStatusPeriodTest {
  val today = LocalDate.now()
  val startDate: LocalDate = LocalDate.parse("2025-07-21") // Monday
  val endDate: LocalDate = LocalDate.parse("2025-07-23") // Wednesday

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
      .hasMessage("The end date must not be before the start date")
  }

  @Test
  fun `should fail to set end date if start date is after end date`() {
    val entity = payStatusPeriod()

    assertThatThrownBy {
      entity.endDate = today.minusDays(1)
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("The end date must not be before the start date")
  }

  @ParameterizedTest(name = "{0} active={1}")
  @CsvSource(
    "2025-07-18, false", // Friday before the start date
    "2025-07-21, true", // start date (Monday)
    "2025-07-22, true", // mid period (Tuesday)
    "2025-07-23, true", // end date (Wednesday) - AC1 active on the end date
    "2025-07-24, false", // day after the end date (Thursday) - AC3 inactive the day after
  )
  fun `pay status period isActiveOn across the period boundary`(date: LocalDate, expected: Boolean) {
    val entity = payStatusPeriod(startDate = startDate, endDate = endDate)

    assertThat(entity.isActiveOn(date)).isEqualTo(expected)
  }

  @ParameterizedTest(name = "{0} active={1}")
  @CsvSource(
    "2025-07-25, true", // Friday within the period - weekday
    "2025-07-26, false", // Saturday within the period - weekend
    "2025-07-27, false", // Sunday within the period - weekend
    "2025-07-28, true", // Monday within the period - weekday
  )
  fun `pay status period should only be active on weekdays within the period`(date: LocalDate, expected: Boolean) {
    val entity = payStatusPeriod(startDate = startDate, endDate = LocalDate.parse("2025-07-31"))

    assertThat(entity.isActiveOn(date)).isEqualTo(expected)
  }

  @Test
  fun `pay status period should be active on the end date when start date equals end date`() {
    val entity = payStatusPeriod(startDate = startDate, endDate = startDate)

    assertThat(entity.isActiveOn(startDate)).isTrue()
  }

  @Test
  fun `should remain active indefinitely when there is no end date`() {
    val entity = payStatusPeriod(startDate = startDate, endDate = null)

    assertThat(entity.isActiveOn(endDate.plusDays(1))).isTrue()
  }

  @ParameterizedTest
  @EnumSource(PayStatusType::class)
  fun `should map each pay status to its corresponding payment type`(type: PayStatusType) {
    assertThat(type.paymentType.name).isEqualTo(type.name)
  }

  @ParameterizedTest
  @EnumSource(PayStatusType::class)
  fun `should return AM and PM sessions for sick periods`(type: PayStatusType) {
    assertThat(payStatusPeriod(type = type).applicableSessions()).containsExactly(TimeSlot.AM, TimeSlot.PM)
  }

  @ParameterizedTest
  @CsvSource(
    "2025-07-18, false, false",
    "2025-07-20, false, false",
    "2025-07-21, true, true",
    "2025-07-22, true, true",
    "2025-07-23, true, true",
    "2025-07-24, false, true",
    "2025-07-26, false, false",
    "2025-07-27, false, false",
    "2025-07-28, false, true",
  )
  fun `STS periods should be active on weekdays within the inclusive date range`(
    date: LocalDate,
    expectedWithEndDate: Boolean,
    expectedWithoutEndDate: Boolean,
  ) {
    val entity = payStatusPeriod(type = PayStatusType.SHORT_TERM_SICK, startDate = startDate, endDate = endDate)
    val openEndedEntity = payStatusPeriod(type = PayStatusType.SHORT_TERM_SICK, startDate = startDate, endDate = null)

    assertThat(entity.isActiveOn(date)).isEqualTo(expectedWithEndDate)
    assertThat(openEndedEntity.isActiveOn(date)).isEqualTo(expectedWithoutEndDate)
  }

  @ParameterizedTest
  @EnumSource(PayStatusType::class)
  fun `open ended sick periods should be active on future weekdays but not weekends`(type: PayStatusType) {
    val entity = payStatusPeriod(type = type, startDate = startDate, endDate = null)

    assertThat(entity.isActiveOn(LocalDate.of(2026, 1, 2))).isTrue()
    assertThat(entity.isActiveOn(LocalDate.of(2026, 1, 3))).isFalse()
    assertThat(entity.isActiveOn(LocalDate.of(2026, 1, 4))).isFalse()
  }
}
