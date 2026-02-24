package uk.gov.justice.digital.hmpps.prisonerpayapi.mapping

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.response.PayRateDto
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.updatePayRateRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayRate
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusType
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

class PayRateMappingTest {
  val clock: Clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("Europe/London"))

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
      updatedDateTime = null,
      updatedBy = null,
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
      updatedDateTime = entity.updatedDateTime,
      updatedBy = entity.updatedBy,
    )

    assertThat(actualResult).isEqualTo(expectedResult)
  }

  @Test
  fun `should map from dto to entity`() {
    val dto = updatePayRateRequest()

    val result = dto.toEntity("TEST_USER", clock)

    with(result) {
      assertThat(id).isNull()
      assertThat(prisonCode).isEqualTo(dto.prisonCode)
      assertThat(type).isEqualTo(dto.type)
      assertThat(startDate).isEqualTo(dto.startDate)
      assertThat(rate).isEqualTo(dto.rate)
      assertThat(createdBy).isEqualTo("TEST_USER")
      assertThat(createdDateTime).isEqualTo(LocalDateTime.now(clock))
      assertThat(updatedBy).isNull()
      assertThat(updatedDateTime).isNull()
    }
  }
}
