package uk.gov.justice.digital.hmpps.prisonerpayapi.service.paystatusperiod.rules

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.createPayStatusPeriodRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusType
import java.time.LocalDate

class ShortTermSickPayStatusPeriodRulesTest {
  private val rules = ShortTermSickPayStatusPeriodRules()
  private val today = LocalDate.of(2026, 1, 1)

  @Test
  fun `should identify the STS pay type`() {
    assertThat(rules.payType()).isEqualTo(PayStatusType.SHORT_TERM_SICK)
  }

  @ParameterizedTest
  @CsvSource("0, 0", "0, 27", "0, 28", "40, 28")
  fun `should allow up to 28 days from the start date`(startOffset: Long, duration: Long) {
    val startDate = today.plusDays(startOffset)
    val request = createPayStatusPeriodRequest(
      type = PayStatusType.SHORT_TERM_SICK,
      startDate = startDate,
      endDate = startDate.plusDays(duration),
    )

    assertThatCode { rules.validateCreateRequest(request, today) }.doesNotThrowAnyException()
  }

  @ParameterizedTest
  @ValueSource(longs = [0, 40])
  fun `should reject more than 28 days from the start date`(startOffset: Long) {
    val startDate = today.plusDays(startOffset)
    val request = createPayStatusPeriodRequest(
      type = PayStatusType.SHORT_TERM_SICK,
      startDate = startDate,
      endDate = startDate.plusDays(29),
    )

    assertThatThrownBy { rules.validateCreateRequest(request, today) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("The pay status period must not exceed 28 days")
  }

// Commented out as this is part of another ticket.
//  @ParameterizedTest
//  @ValueSource(longs = [-1, 0])
//  fun `should reject deleting past or current STS periods`(startOffset: Long) {
//    val existing = payStatusPeriod(type = PayStatusType.SHORT_TERM_SICK, startDate = today.plusDays(startOffset), endDate = null)
//
//    assertThatThrownBy { rules.validateDeleteRequest(existing, today) }
//      .isInstanceOf(IllegalArgumentException::class.java)
//      .hasMessage("Cannot delete a pay status period with a start date in the past or today")
//  }

//  @Test
//  fun `should allow deleting a future STS period`() {
//    val existing = payStatusPeriod(type = PayStatusType.SHORT_TERM_SICK, startDate = today.plusDays(1), endDate = null)
//
//    assertThatCode { rules.validateDeleteRequest(existing, today) }.doesNotThrowAnyException()
//  }
}
