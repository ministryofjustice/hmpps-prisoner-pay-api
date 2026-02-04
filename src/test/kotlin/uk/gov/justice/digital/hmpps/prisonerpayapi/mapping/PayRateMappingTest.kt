package uk.gov.justice.digital.hmpps.prisonerpayapi.mapping

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.response.PayRateDto
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayRate
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class PayRateMappingTest {

  @Test
  fun `should map from pay rate entity to dto`() {
    val entity = PayRate(
      id = UUID.randomUUID(),
      prisonCode = "BCI",
      type = PayStatusType.LONG_TERM_SICK,
      startDate = LocalDate.of(2026, 1, 27),
      rate = 99,
      createdDateTime = LocalDateTime.of(2026, 1, 27, 10, 20),
      createdBy = "TEST_USER",
    )

    val actualResult = entity.toModel()

    val expectedResult = PayRateDto(
      id = entity.id!!,
      prisonCode = entity.prisonCode,
      type = entity.type,
      startDate = entity.startDate,
      rate = entity.rate,
      createdDateTime = entity.createdDateTime,
      createdBy = entity.createdBy,
    )

    assertThat(actualResult).isEqualTo(expectedResult)
  }
}
