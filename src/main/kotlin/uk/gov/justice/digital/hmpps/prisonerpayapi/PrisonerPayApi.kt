package uk.gov.justice.digital.hmpps.prisonerpayapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PrisonerPayApi

fun main(args: Array<String>) {
  runApplication<PrisonerPayApi>(*args)
}
