package uk.gov.justice.digital.hmpps.prisonerpayapi.service.paystatusperiod

import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.createPayStatusPeriodRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusType
import java.time.LocalDate

class BasePayStatusPeriodRulesTest {
  private val today = LocalDate.of(2026, 1, 1)

  private fun rules(maxDurationDays: Long? = null) = object : BasePayStatusPeriodRules(maxDurationDays) {
    override fun payType() = PayStatusType.LONG_TERM_SICK
  }

  @Test
  fun `should allow a null end date by default`() {
    val request = createPayStatusPeriodRequest(startDate = today, endDate = null)

    assertThatCode { rules().validateCreateRequest(request, today) }.doesNotThrowAnyException()
  }

  @Test
  fun `should allow a period up to the maximum duration`() {
    val request = createPayStatusPeriodRequest(startDate = today, endDate = today.plusDays(28))

    assertThatCode { rules(maxDurationDays = 28).validateCreateRequest(request, today) }.doesNotThrowAnyException()
  }

  @Test
  fun `should reject a period that exceeds the maximum duration`() {
    val request = createPayStatusPeriodRequest(startDate = today, endDate = today.plusDays(29))

    assertThatThrownBy { rules(maxDurationDays = 28).validateCreateRequest(request, today) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("The pay status period must not exceed 28 days")
  }

  @Test
  fun `should not apply the maximum duration when no end date is set`() {
    val request = createPayStatusPeriodRequest(startDate = today, endDate = null)

    assertThatCode { rules(maxDurationDays = 28).validateCreateRequest(request, today) }.doesNotThrowAnyException()
  }
}
