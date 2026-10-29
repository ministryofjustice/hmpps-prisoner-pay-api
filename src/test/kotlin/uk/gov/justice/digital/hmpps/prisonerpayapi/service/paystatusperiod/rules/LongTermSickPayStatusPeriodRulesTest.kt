package uk.gov.justice.digital.hmpps.prisonerpayapi.service.paystatusperiod.rules

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.createPayStatusPeriodRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.payStatusPeriod
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusType
import java.time.LocalDate

class LongTermSickPayStatusPeriodRulesTest {
  private val rules = LongTermSickPayStatusPeriodRules()
  private val today = LocalDate.of(2026, 1, 1)

  @Test
  fun `should identify the LTS pay type`() {
    assertThat(rules.payType()).isEqualTo(PayStatusType.LONG_TERM_SICK)
  }

  @ParameterizedTest
  @ValueSource(longs = [0, 1, 30, 3650])
  fun `should allow today and any future start date when creating`(daysInFuture: Long) {
    val startDate = today.plusDays(daysInFuture)
    val request = createPayStatusPeriodRequest(startDate = startDate, endDate = startDate.plusMonths(1))

    assertThatCode { rules.validateCreateRequest(request, today) }.doesNotThrowAnyException()
  }

  @Test
  fun `should allow a null end date when creating`() {
    val request = createPayStatusPeriodRequest(startDate = today, endDate = null)

    assertThatCode { rules.validateCreateRequest(request, today) }.doesNotThrowAnyException()
  }

  @Test
  fun `should throw exception for start date in the past when creating`() {
    val request = createPayStatusPeriodRequest(startDate = today.minusDays(1), endDate = today.plusMonths(1))

    assertThatThrownBy { rules.validateCreateRequest(request, today) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("The start date must not be in the past")
  }

  @Test
  fun `should throw exception for end date in the past when creating`() {
    val request = createPayStatusPeriodRequest(startDate = today, endDate = today.minusDays(1))

    assertThatThrownBy { rules.validateCreateRequest(request, today) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("The end date must not be in the past")
  }

  @Test
  fun `should throw exception for end date before start date when creating`() {
    val request = createPayStatusPeriodRequest(startDate = today.plusDays(30), endDate = today.plusDays(10))

    assertThatThrownBy { rules.validateCreateRequest(request, today) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("The end date must not be before the start date")
  }

  @ParameterizedTest
  @ValueSource(longs = [-1, 0])
  fun `should throw exception when deleting a past or current period`(daysFromToday: Long) {
    val existing = payStatusPeriod(startDate = today.plusDays(daysFromToday), endDate = today.plusMonths(1))

    assertThatThrownBy { rules.validateDeleteRequest(existing, today) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Cannot delete a pay status period with a start date in the past or today")
  }

  @Test
  fun `should allow deleting a future period`() {
    val existing = payStatusPeriod(startDate = today.plusDays(1), endDate = today.plusMonths(1))

    assertThatCode { rules.validateDeleteRequest(existing, today) }.doesNotThrowAnyException()
  }
}
