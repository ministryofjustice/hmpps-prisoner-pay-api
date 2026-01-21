package uk.gov.justice.digital.hmpps.prisonerpayapi.mapping

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.prisonerpayapi.helper.createPayStatusPeriodRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusType
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.response.PayStatusPeriod as Dto
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusPeriod as Entity

class PayStatusPeriodMappingTest {
  val clock: Clock = Clock.fixed(Instant.parse("2025-07-23T12:34:56Z"), ZoneId.of("Europe/London"))

  @Test
  fun `should map from entity to dto`() {
    val entity = Entity(
      id = UUID.randomUUID(),
      prisonCode = "RSI",
      prisonerNumber = "A1234AA",
      type = PayStatusType.LONG_TERM_SICK,
      startDate = LocalDate.now(),
      endDate = LocalDate.now().plusDays(10),
      createdDateTime = LocalDateTime.now(),
      createdBy = "BLOGGSJ",
    )

    val result = entity.toModel()

    val expectedDto = Dto(
      id = entity.id!!,
      prisonCode = entity.prisonCode,
      prisonerNumber = entity.prisonerNumber,
      type = entity.type,
      startDate = entity.startDate,
      endDate = entity.endDate,
      createdDateTime = entity.createdDateTime,
      createdBy = entity.createdBy,
    )

    assertThat(result).isEqualTo(expectedDto)
  }

  @Test
  fun `should map from dto to entity`() {
    val dto = createPayStatusPeriodRequest()

    val result = dto.toEntity("JBLOGGS", clock)

    with(result) {
      assertThat(id).isNull()
      assertThat(prisonCode).isEqualTo(dto.prisonCode)
      assertThat(prisonerNumber).isEqualTo(dto.prisonerNumber)
      assertThat(type).isEqualTo(dto.type)
      assertThat(startDate).isEqualTo(dto.startDate)
      assertThat(endDate).isEqualTo(dto.endDate)
      assertThat(createdBy).isEqualTo("JBLOGGS")
      assertThat(createdDateTime).isEqualTo(LocalDateTime.now(clock))
    }
  }
}
