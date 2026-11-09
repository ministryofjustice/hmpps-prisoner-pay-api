package uk.gov.justice.digital.hmpps.prisonerpayapi.service.paystatusperiod

import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Named
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request.UpdatePayStatusPeriodRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.createPayStatusPeriodRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.payStatusPeriod
import uk.gov.justice.digital.hmpps.prisonerpayapi.service.paystatusperiod.rules.LongTermSickPayStatusPeriodRules
import uk.gov.justice.digital.hmpps.prisonerpayapi.service.paystatusperiod.rules.ShortTermSickPayStatusPeriodRules
import java.time.LocalDate

class BasePayStatusPeriodRulesTest {
  private val today = LocalDate.of(2026, 1, 1)

  companion object {
    @JvmStatic
    fun rules(): List<PayStatusPeriodRules> = listOf(LongTermSickPayStatusPeriodRules(), ShortTermSickPayStatusPeriodRules())

    @JvmStatic
    fun rulesWithFutureStartDates() = rules().flatMap { rules ->
      listOf(0L, 1L, 30L, 3650L).map { Arguments.of(rules, it) }
    }

    @JvmStatic
    fun rulesWithEndDateOptions() = rules().flatMap { rules ->
      listOf(false, true).map { Arguments.of(Named.of(rules.payType().name, rules), it) }
    }

    @JvmStatic
    fun rulesWithPastOrCurrentStartDates() = rules().flatMap { rules ->
      listOf(-1L, 0L).flatMap { startOffset ->
        listOf(false, true).map { Arguments.of(Named.of(rules.payType().name, rules), startOffset, it) }
      }
    }
  }

  @ParameterizedTest
  @MethodSource("rulesWithFutureStartDates")
  fun `should allow today and any future start date when creating`(rules: PayStatusPeriodRules, daysInFuture: Long) {
    val startDate = today.plusDays(daysInFuture)
    val request = createPayStatusPeriodRequest(type = rules.payType(), startDate = startDate, endDate = startDate.plusDays(28))

    assertThatCode { rules.validateCreateRequest(request, today) }.doesNotThrowAnyException()
  }

  @ParameterizedTest
  @MethodSource("rules")
  fun `should allow a null end date by default`(rules: PayStatusPeriodRules) {
    val request = createPayStatusPeriodRequest(type = rules.payType(), startDate = today, endDate = null)

    assertThatCode { rules.validateCreateRequest(request, today) }.doesNotThrowAnyException()
  }

  @ParameterizedTest
  @MethodSource("rules")
  fun `should throw an exception when start date is in the past when creating`(rules: PayStatusPeriodRules) {
    val request = createPayStatusPeriodRequest(type = rules.payType(), startDate = today.minusDays(1), endDate = today.plusDays(1))

    assertThatThrownBy { rules.validateCreateRequest(request, today) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("The start date must not be in the past")
  }

  @ParameterizedTest
  @MethodSource("rules")
  fun `should throw an exception when end date is in the past when creating`(rules: PayStatusPeriodRules) {
    val request = createPayStatusPeriodRequest(type = rules.payType(), startDate = today, endDate = today.minusDays(1))

    assertThatThrownBy { rules.validateCreateRequest(request, today) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("The end date must not be in the past")
  }

  @ParameterizedTest
  @MethodSource("rules")
  fun `should throw an exception when end date is before the start date when creating`(rules: PayStatusPeriodRules) {
    val request = createPayStatusPeriodRequest(type = rules.payType(), startDate = today.plusDays(30), endDate = today.plusDays(10))

    assertThatThrownBy { rules.validateCreateRequest(request, today) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("The end date must not be before the start date")
  }

  @ParameterizedTest
  @MethodSource("rules")
  fun `should allow an end date equal to the start date`(rules: PayStatusPeriodRules) {
    val request = createPayStatusPeriodRequest(type = rules.payType(), startDate = today, endDate = today)

    assertThatCode { rules.validateCreateRequest(request, today) }.doesNotThrowAnyException()
  }

  @ParameterizedTest
  @MethodSource("rules")
  fun `should reject conflicting fields when updating`(rules: PayStatusPeriodRules) {
    val existing = payStatusPeriod(type = rules.payType(), startDate = today, endDate = null)
    val request = UpdatePayStatusPeriodRequest(endDate = today, removeEndDate = true)

    assertThatThrownBy { rules.validateUpdateRequest(existing, request, today) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("removeEndDate cannot be true when an endDate is also supplied")
  }

  @ParameterizedTest
  @MethodSource("rules")
  fun `should reject a past end date when updating`(rules: PayStatusPeriodRules) {
    val existing = payStatusPeriod(type = rules.payType(), startDate = today.minusDays(10), endDate = null)
    val request = UpdatePayStatusPeriodRequest(endDate = today.minusDays(1))

    assertThatThrownBy { rules.validateUpdateRequest(existing, request, today) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("The end date must not be in the past")
  }

  @ParameterizedTest
  @MethodSource("rules")
  fun `should reject an end date before the start date when updating`(rules: PayStatusPeriodRules) {
    val existing = payStatusPeriod(type = rules.payType(), startDate = today.plusDays(10), endDate = null)
    val request = UpdatePayStatusPeriodRequest(endDate = today.plusDays(5))

    assertThatThrownBy { rules.validateUpdateRequest(existing, request, today) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("The end date must not be before the start date")
  }

  @ParameterizedTest
  @MethodSource("rules")
  fun `should reject extending or reopening expired periods`(rules: PayStatusPeriodRules) {
    val existing = payStatusPeriod(type = rules.payType(), startDate = today.minusDays(10), endDate = today.minusDays(1))
    val requests = listOf(UpdatePayStatusPeriodRequest(endDate = today), UpdatePayStatusPeriodRequest(removeEndDate = true))

    requests.forEach { request ->
      assertThatThrownBy { rules.validateUpdateRequest(existing, request, today) }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Cannot update an expired pay status period")
    }
  }

  @ParameterizedTest
  @MethodSource("rules")
  fun `should allow updating a period ending today`(rules: PayStatusPeriodRules) {
    val existing = payStatusPeriod(type = rules.payType(), startDate = today.minusDays(10), endDate = today)

    assertThatCode { rules.validateUpdateRequest(existing, UpdatePayStatusPeriodRequest(endDate = today.plusDays(1)), today) }
      .doesNotThrowAnyException()
  }

  @ParameterizedTest(name = "{0}, start offset={1}, open ended={2}")
  @MethodSource("rulesWithPastOrCurrentStartDates")
  fun `should throw an exception when deleting a past or current period`(
    rules: PayStatusPeriodRules,
    daysFromToday: Long,
    openEnded: Boolean,
  ) {
    val existing = payStatusPeriod(
      type = rules.payType(),
      startDate = today.plusDays(daysFromToday),
      endDate = if (openEnded) null else today.plusDays(10),
    )

    assertThatThrownBy { rules.validateDeleteRequest(existing, today) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Cannot delete a pay status period with a start date in the past or today")
  }

  @ParameterizedTest(name = "{0}, open ended={1}")
  @MethodSource("rulesWithEndDateOptions")
  fun `should allow deleting a future period`(rules: PayStatusPeriodRules, openEnded: Boolean) {
    val existing = payStatusPeriod(
      type = rules.payType(),
      startDate = today.plusDays(1),
      endDate = if (openEnded) null else today.plusDays(10),
    )

    assertThatCode { rules.validateDeleteRequest(existing, today) }.doesNotThrowAnyException()
  }
}
