package uk.gov.justice.digital.hmpps.prisonerpayapi.common

import java.time.DayOfWeek
import java.time.LocalDate

val LocalDate.isWeekDay: Boolean
  get() = dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY
