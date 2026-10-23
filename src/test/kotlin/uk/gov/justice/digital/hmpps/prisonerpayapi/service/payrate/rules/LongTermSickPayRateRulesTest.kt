package uk.gov.justice.digital.hmpps.prisonerpayapi.service.payrate.rules

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.payRate
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.updatePayRateRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusType
import java.time.LocalDate

class LongTermSickPayRateRulesTest {
  private val rules = LongTermSickPayRateRules()
  private val today = LocalDate.of(2026, 1, 1)

  @Test
  fun `should identify the LTS pay type`() {
    assertThat(rules.payType()).isEqualTo(PayStatusType.LONG_TERM_SICK)
  }

  @ParameterizedTest
  @ValueSource(longs = [0, 1, 30, 31, 3650])
  fun `should allow today and any future effective date when updating pay rate`(daysInFuture: Long) {
    val request = updatePayRateRequest(startDate = today.plusDays(daysInFuture))

    assertThatCode {
      rules.validateUpdateRequest(payRate(startDate = today.minusDays(1)), request, today)
      rules.validateUpdateRequest(payRate(startDate = today), request, today)
    }.doesNotThrowAnyException()
  }

  @Test
  fun `should throw exception for start date in the past when updating pay rate`() {
    val request = updatePayRateRequest(startDate = today.minusDays(1))

    assertThatThrownBy { rules.validateUpdateRequest(payRate(startDate = today), request, today) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Pay rate start date must not be in the past")
  }

  @Test
  fun `should throw exception when updating an existing future rate`() {
    val request = updatePayRateRequest(startDate = today.plusDays(10))

    assertThatThrownBy { rules.validateUpdateRequest(payRate(startDate = today.plusDays(1)), request, today) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Future pay rate must be cancelled before updating")
  }

  @ParameterizedTest
  @ValueSource(longs = [-1, 0])
  fun `should throw exception when deleting past and current rates`(daysFromToday: Long) {
    assertThatThrownBy { rules.validateDeleteRequest(payRate(startDate = today.plusDays(daysFromToday)), today) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Cannot delete a pay rate with a start date in the past or today")
  }

  @Test
  fun `should allow deleting a future rate`() {
    assertThatCode {
      rules.validateDeleteRequest(payRate(startDate = today.plusDays(1)), today)
    }.doesNotThrowAnyException()
  }
}
