package uk.gov.justice.digital.hmpps.prisonerpayapi.helper

import java.time.LocalDate
import java.time.ZoneId

fun today(): LocalDate = LocalDate.now(ZoneId.of("Europe/London"))
fun yesterday(): LocalDate = today().minusDays(1)
fun tomorrow(): LocalDate = today().plusDays(1)
