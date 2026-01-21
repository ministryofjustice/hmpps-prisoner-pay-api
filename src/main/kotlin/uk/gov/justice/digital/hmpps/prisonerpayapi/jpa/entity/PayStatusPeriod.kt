package uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import uk.gov.justice.digital.hmpps.prisonerpayapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.prisonerpayapi.common.isWeekDay
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Entity
class PayStatusPeriod(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: UUID? = null,

  val prisonerNumber: String,

  @Enumerated(EnumType.STRING)
  val type: PayStatusType,

  val startDate: LocalDate,

  endDate: LocalDate? = null,

  val createdDateTime: LocalDateTime,

  val createdBy: String,

  val prisonCode: String,
) {
  var endDate: LocalDate? = endDate
    set(value) {
      require(value == null || value >= startDate) { "endDate cannot be before startDate" }

      field = value
    }

  init {
    this.endDate = endDate
  }

  fun applicableSessions() = when (type) {
    PayStatusType.LONG_TERM_SICK -> listOf(TimeSlot.AM, TimeSlot.PM)
  }

  fun isActiveOn(date: LocalDate): Boolean {
    val isInDateRange = date >= startDate && (endDate == null || date <= endDate)

    return isInDateRange &&
      when (type) {
        PayStatusType.LONG_TERM_SICK -> date.isWeekDay
      }
  }
}
