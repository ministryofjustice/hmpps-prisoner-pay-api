package uk.gov.justice.digital.hmpps.prisonerpayapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.response.PayRateDto
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.repository.PayRateRepository
import uk.gov.justice.digital.hmpps.prisonerpayapi.mapping.toModel

@Service
class PayRateService(
  private val payRateRepository: PayRateRepository,
) {
  private val logger: Logger = LoggerFactory.getLogger(PayRateService::class.java)

  fun getCurrentAndFuturePayRatesByPrisonCode(prisonCode: String): List<PayRateDto> {
    val payRates = payRateRepository.getCurrentAndFuturePayRatesByPrisonCode(prisonCode)
    println("Pay rates from repository: ${payRates.toList()}")
    if (payRates.isEmpty()) {
      logger.info("No pay rates found for prison $prisonCode")
      return emptyList()
    }
    return payRates.map { it.toModel() }
  }
}
