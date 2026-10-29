package uk.gov.justice.digital.hmpps.prisonerpayapi.service.payrate

import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.payRate
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.updatePayRateRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusType
import java.time.LocalDate

class BasePayRateRulesTest {
  private val today = LocalDate.of(2026, 1, 1)

  private val rules = object : BasePayRateRules() {
    override fun payType() = PayStatusType.LONG_TERM_SICK
  }

  @Test
  fun `should allow updating a current rate with a start date of today`() {
    val request = updatePayRateRequest(startDate = today)

    assertThatCode { rules.validateUpdateRequest(payRate(startDate = today), request, today) }.doesNotThrowAnyException()
  }

  @Test
  fun `should allow updating a past rate with a future start date`() {
    val request = updatePayRateRequest(startDate = today.plusDays(10))

    assertThatCode { rules.validateUpdateRequest(payRate(startDate = today.minusDays(1)), request, today) }.doesNotThrowAnyException()
  }

  @Test
  fun `should throw an exception when updating with a start date in the past`() {
    val request = updatePayRateRequest(startDate = today.minusDays(1))

    assertThatThrownBy { rules.validateUpdateRequest(payRate(startDate = today), request, today) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Pay rate start date must not be in the past")
  }

  @Test
  fun `should throw an exception when updating an existing future rate`() {
    val request = updatePayRateRequest(startDate = today.plusDays(10))

    assertThatThrownBy { rules.validateUpdateRequest(payRate(startDate = today.plusDays(1)), request, today) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Future pay rate must be cancelled before updating")
  }

  @Test
  fun `should allow deleting a future rate`() {
    assertThatCode { rules.validateDeleteRequest(payRate(startDate = today.plusDays(1)), today) }.doesNotThrowAnyException()
  }

  @Test
  fun `should throw an exception when deleting a rate with a start date of today`() {
    assertThatThrownBy { rules.validateDeleteRequest(payRate(startDate = today), today) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Cannot delete a pay rate with a start date in the past or today")
  }

  @Test
  fun `should throw an exception when deleting a rate with a start date in the past`() {
    assertThatThrownBy { rules.validateDeleteRequest(payRate(startDate = today.minusDays(1)), today) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Cannot delete a pay rate with a start date in the past or today")
  }
}
