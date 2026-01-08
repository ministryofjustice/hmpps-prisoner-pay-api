package uk.gov.justice.digital.hmpps.prisonerpayapi.job

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class JobRunnerTest {
  val handler: JobHandler = mock()
  val jobRunner = JobRunner(listOf(handler))

  @BeforeEach
  fun setUp() {
    whenever(handler.jobType()).thenReturn(JobType.MAKE_PAYMENTS)
  }

  @Test
  fun `should call execute on matched handler`() {
    jobRunner.run(JobType.MAKE_PAYMENTS.name)

    verify(handler).jobType()
    verify(handler).execute()
  }

  @Test
  fun `should return a handler when name is not matched`() {
    assertThatThrownBy {
      jobRunner.run("Blah")
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Unrecognised job name: Blah")

    verify(handler).jobType()
    verify(handler, never()).execute()
  }
}
