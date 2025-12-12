package uk.gov.justice.digital.hmpps.prisonerpayapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request.UpdatePayStatusPeriodRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.response.PayStatusPeriod
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.repository.PayStatusPeriodRepository
import uk.gov.justice.digital.hmpps.prisonerpayapi.mapping.toModel
import java.util.*
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusPeriod as PayStatusPeriodEntity

@Service
class PayStatusPeriodUpdateService(
  private val repository: PayStatusPeriodRepository,
) {
  @Transactional
  fun update(id: UUID, request: UpdatePayStatusPeriodRequest): PayStatusPeriod {
    require(request.endDate == null || !request.removeEndDate) {
      "removeEndDate cannot be true when an endDate is also supplied"
    }

    return repository
      .findById(id)
      .orElseThrow { EntityNotFoundException("Pay Period Status with id '$id' not found") }
      .also { applyEndDateUpdate(it, request) }
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
