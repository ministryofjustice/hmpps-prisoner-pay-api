package uk.gov.justice.digital.hmpps.prisonerpayapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request.UpdatePayStatusPeriodRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.response.PayStatusPeriod
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.repository.PayStatusPeriodRepository
import uk.gov.justice.digital.hmpps.prisonerpayapi.mapping.toModel
import uk.gov.justice.digital.hmpps.prisonerpayapi.service.paystatusperiod.BasePayStatusPeriodRules
import uk.gov.justice.digital.hmpps.prisonerpayapi.service.paystatusperiod.PayStatusPeriodRulesRegistry
import java.time.Clock
import java.time.LocalDate
import java.util.*
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusPeriod as PayStatusPeriodEntity

@Service
class PayStatusPeriodUpdateService(
  private val repository: PayStatusPeriodRepository,
  private val clock: Clock,
  private val rulesRegistry: PayStatusPeriodRulesRegistry,
) {
  @Transactional
  fun update(id: UUID, request: UpdatePayStatusPeriodRequest): PayStatusPeriod {
    val today = LocalDate.now(clock)
    BasePayStatusPeriodRules.validateUpdateRequestFields(request, today)

    return repository
      .findById(id)
      .orElseThrow { EntityNotFoundException("Pay Period Status with id '$id' not found") }
      .also { entity ->
        rulesRegistry.rulesFor(entity.type).validateUpdateRequest(entity, request, today)
        applyEndDateUpdate(entity, request)
      }
      .let { repository.saveAndFlush(it) }
      .toModel()
  }

  private fun applyEndDateUpdate(payStatusPeriod: PayStatusPeriodEntity, request: UpdatePayStatusPeriodRequest) {
    if (request.removeEndDate) {
      payStatusPeriod.endDate = null
    } else {
      request.endDate?.let { newEndDate ->
        payStatusPeriod.endDate = newEndDate
      }
    }
  }
}
