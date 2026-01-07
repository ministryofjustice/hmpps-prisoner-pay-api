package uk.gov.justice.digital.hmpps.prisonerpayapi.job

interface JobHandler {
  fun jobType(): JobType
  fun execute()
}

enum class JobType {
  MAKE_PAYMENTS,
}
