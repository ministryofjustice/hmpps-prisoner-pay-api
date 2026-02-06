package uk.gov.justice.digital.hmpps.prisonerpayapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.repository.PayRateRepository
import uk.gov.justice.digital.hmpps.prisonerpayapi.mapping.toModel

@Service
class PayRateService(
  private val payRateRepository: PayRateRepository,
) {
  fun getCurrentAndFuturePayRatesByPrisonCode(prisonCode: String) = payRateRepository
    .getCurrentAndFuturePayRatesByPrisonCode(prisonCode)
    .map { it.toModel() }
}
