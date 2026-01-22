package uk.gov.justice.digital.hmpps.prisonerpayapi.common

import java.time.DayOfWeek
import java.time.LocalDate

fun today(): LocalDate = LocalDate.now()
fun yesterday(): LocalDate = LocalDate.now().minusDays(1)

val LocalDate.isWeekDay: Boolean
  get() = dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY
