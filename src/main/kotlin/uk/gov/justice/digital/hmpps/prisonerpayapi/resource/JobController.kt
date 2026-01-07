package uk.gov.justice.digital.hmpps.prisonerpayapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.prisonerpayapi.job.JobRunner

@Tag(name = "Job Controller")
@RestController
@ProtectedByIngress
@RequestMapping(value = ["job-admin"], produces = [MediaType.APPLICATION_JSON_VALUE])
class JobController(
  private val jobRunner: JobRunner,
) {
  @Operation(summary = "Endpoint to trigger a job, usually fron a cron.")
  @PostMapping(path = ["/run/{jobName}"])
  @ResponseStatus(HttpStatus.OK)
  fun runJob(@PathVariable jobName: String) = jobRunner.run(jobName)
}
