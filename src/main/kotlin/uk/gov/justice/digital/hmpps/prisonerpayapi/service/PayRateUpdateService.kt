package uk.gov.justice.digital.hmpps.prisonerpayapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prisonerpayapi.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request.UpdatePayRateRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.response.PayRateDto
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayRate
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.repository.PayRateRepository
import uk.gov.justice.digital.hmpps.prisonerpayapi.mapping.toEntity
import uk.gov.justice.digital.hmpps.prisonerpayapi.mapping.toModel
import uk.gov.justice.hmpps.kotlin.auth.HmppsAuthenticationHolder
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Service
class PayRateUpdateService(
  private val authenticationHolder: HmppsAuthenticationHolder,
  private val payRateRepository: PayRateRepository,
  private val clock: Clock,
) {

  @Transactional
  fun update(id: UUID, request: UpdatePayRateRequest): PayRateDto {
    val currentUser = authenticationHolder.username ?: SYSTEM_USERNAME
    val today = LocalDate.now(clock)

    val existing = payRateRepository.findById(id)
      .orElseThrow { EntityNotFoundException("Pay rate with id '$id' not found") }

    return when {
      existing.startDate.isBefore(today) -> createNewPayRate(request, currentUser)
      existing.startDate == today -> handleTodayRate(existing, request, today, currentUser)
      else -> replaceFutureRate(existing, request, currentUser)
    }
  }

  private fun createNewPayRate(request: UpdatePayRateRequest, createdBy: String): PayRateDto = request
    .also { checkNoDuplicatesExist(it) }
    .toEntity(createdBy, clock)
    .let { payRateRepository.save(it) }
    .toModel()

  private fun handleTodayRate(existing: PayRate, request: UpdatePayRateRequest, today: LocalDate, user: String): PayRateDto = if (request.startDate == today) {
    existing.apply {
      rate = request.rate
      updatedBy = user
      updatedDateTime = LocalDateTime.now(clock)
    }
      .let { payRateRepository.save(it) }
      .toModel()
  } else {
    createNewPayRate(request, user)
  }

  private fun replaceFutureRate(existing: PayRate, request: UpdatePayRateRequest, createdBy: String): PayRateDto = existing
    .also { payRateRepository.delete(it) }
    .let { createNewPayRate(request, createdBy) }

  private fun checkNoDuplicatesExist(request: UpdatePayRateRequest) {
    if (payRateRepository.existsByPrisonCodeAndTypeAndStartDate(
        request.prisonCode,
        request.type,
        request.startDate,
      )
    ) {
      throw IllegalArgumentException(
        "Pay rate already exists for prison: ${request.prisonCode}, type: ${request.type} on ${request.startDate}",
      )
    }
  }
}
