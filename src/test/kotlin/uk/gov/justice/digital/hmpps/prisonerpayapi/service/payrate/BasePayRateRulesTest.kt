package uk.gov.justice.digital.hmpps.prisonerpayapi.service.payrate

import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.payRate
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.updatePayRateRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusType
import java.time.LocalDate

class BasePayRateRulesTest {
  private val today = LocalDate.of(2026, 1, 1)

  private fun rulesFor(type: PayStatusType) = object : BasePayRateRules() {
    override fun payType() = type
  }

  @ParameterizedTest
  @EnumSource(PayStatusType::class)
  fun `should allow updating a current rate with a start date of today`(type: PayStatusType) {
    val rules = rulesFor(type)
    val request = updatePayRateRequest(startDate = today)

    assertThatCode { rules.validateUpdateRequest(payRate(type = type, startDate = today), request, today) }.doesNotThrowAnyException()
  }

  @ParameterizedTest
  @EnumSource(PayStatusType::class)
  fun `should allow updating a past rate with a future start date`(type: PayStatusType) {
    val rules = rulesFor(type)
    val request = updatePayRateRequest(startDate = today.plusDays(10))

    assertThatCode { rules.validateUpdateRequest(payRate(type = type, startDate = today.minusDays(1)), request, today) }.doesNotThrowAnyException()
  }

  @ParameterizedTest
  @EnumSource(PayStatusType::class)
  fun `should throw an exception when updating with a start date in the past`(type: PayStatusType) {
    val rules = rulesFor(type)
    val request = updatePayRateRequest(startDate = today.minusDays(1))

    assertThatThrownBy { rules.validateUpdateRequest(payRate(type = type, startDate = today), request, today) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Pay rate start date must not be in the past")
  }

  @ParameterizedTest
  @EnumSource(PayStatusType::class)
  fun `should throw an exception when updating an existing future rate`(type: PayStatusType) {
    val rules = rulesFor(type)
    val request = updatePayRateRequest(startDate = today.plusDays(10))

    assertThatThrownBy { rules.validateUpdateRequest(payRate(type = type, startDate = today.plusDays(1)), request, today) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Future pay rate must be cancelled before updating")
  }

  // Refactored for STS delete pay rate. This comment needs to be removed when the STS delete pay rate is implemented.
  @ParameterizedTest
  @EnumSource(PayStatusType::class)
  fun `should allow deleting a future rate`(type: PayStatusType) {
    val rules = rulesFor(type)
    assertThatCode { rules.validateDeleteRequest(payRate(type = type, startDate = today.plusDays(1)), today) }.doesNotThrowAnyException()
  }

  // Refactored for STS delete pay rate. This comment needs to be removed when the STS delete pay rate is implemented.
  @ParameterizedTest
  @EnumSource(PayStatusType::class)
  fun `should throw an exception when deleting a rate with a start date of today`(type: PayStatusType) {
    val rules = rulesFor(type)
    assertThatThrownBy { rules.validateDeleteRequest(payRate(type = type, startDate = today), today) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Cannot delete a pay rate with a start date in the past or today")
  }

  // Refactored for STS delete pay rate. This comment needs to be removed when the STS delete pay rate is implemented.
  @ParameterizedTest
  @EnumSource(PayStatusType::class)
  fun `should throw an exception when deleting a rate with a start date in the past`(type: PayStatusType) {
    val rules = rulesFor(type)
    assertThatThrownBy { rules.validateDeleteRequest(payRate(type = type, startDate = today.minusDays(1)), today) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Cannot delete a pay rate with a start date in the past or today")
  }
}
