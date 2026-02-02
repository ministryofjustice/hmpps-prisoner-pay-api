package uk.gov.justice.digital.hmpps.prisonerpayapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.response.PayRateDto
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.repository.PayRateRepository
import uk.gov.justice.digital.hmpps.prisonerpayapi.mapping.toModel

@Service
class PayRateService(
  private val payRateRepository: PayRateRepository,
) {
  fun getLongTermSickPayRates(): List<PayRateDto> = payRateRepository
    .findCurrentAndFutureLongTermSickPayRates()
    .map { it.toModel() }
}
