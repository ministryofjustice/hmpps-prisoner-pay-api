package uk.gov.justice.digital.hmpps.prisonerpayapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request.UpdatePayRateRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.response.PayRateDto
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.repository.PayRateRepository
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.prisonerpayapi.mapping.toModel
import uk.gov.justice.digital.hmpps.prisonerpayapi.service.payrate.PayRateRulesRegistry
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

@Service
class PayRateService(
  private val payRateRepository: PayRateRepository,
  private val payRateUpdateService: PayRateUpdateService,
  private val clock: Clock,
  private val payRateRulesRegistry: PayRateRulesRegistry,
) {
  fun getCurrentAndFuturePayRatesByPrisonCode(prisonCode: String) = payRateRepository
    .getCurrentAndFuturePayRatesByPrisonCode(prisonCode, LocalDate.now(clock))
    .map { it.toModel() }

  @Transactional
  fun update(id: UUID, request: UpdatePayRateRequest): PayRateDto = payRateUpdateService.update(id, request)

  fun delete(id: UUID) {
    payRateRepository.findOrThrowNotFound(id)
      .apply {
        payRateRulesRegistry.rulesFor(type).validateDeleteRequest(this, LocalDate.now(clock))
      }
      .also { payRateRepository.delete(it) }
  }
}
