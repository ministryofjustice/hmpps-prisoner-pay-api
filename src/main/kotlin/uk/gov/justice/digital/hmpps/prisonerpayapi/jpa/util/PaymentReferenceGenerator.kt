package uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.util

fun generateReference(): String {
  val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
  return (1..12)
    .map { allowedChars.random() }
    .joinToString("")
}
