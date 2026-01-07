package uk.gov.justice.digital.hmpps.prisonerpayapi.job

import org.springframework.stereotype.Component

@Component
class JobRunner(
  private val jobHandlers: List<JobHandler>,
) {
  fun run(jobName: String) {
    jobHandlers.find { it.jobType().name == jobName }?.execute() ?: throw IllegalArgumentException("Unrecognised job name: $jobName")
  }
}
