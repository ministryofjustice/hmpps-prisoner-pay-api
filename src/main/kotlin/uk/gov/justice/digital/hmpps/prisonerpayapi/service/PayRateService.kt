package uk.gov.justice.digital.hmpps.prisonerpayapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.repository.PayRateRepository
import uk.gov.justice.digital.hmpps.prisonerpayapi.mapping.toModel
import java.time.Clock
import java.time.LocalDate

@Service
class PayRateService(
  private val payRateRepository: PayRateRepository,
  private val clock: Clock,
) {
  fun getCurrentAndFuturePayRatesByPrisonCode(prisonCode: String) = payRateRepository
    .getCurrentAndFuturePayRatesByPrisonCode(prisonCode, LocalDate.now(clock))
    .map { it.toModel() }
}
