package uk.gov.justice.digital.hmpps.prisonerpayapi.helper

import java.time.LocalDate

fun yesterday(): LocalDate = today().minusDays(1)
fun today(): LocalDate = LocalDate.now()
fun tomorrow(): LocalDate = today().plusDays(1)
