package uk.gov.justice.digital.hmpps.prisonerpayapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prisonerpayapi.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request.UpdatePayRateRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.response.PayRateDto
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayRate
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusType
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
    val maxAllowedStartDate = today.plusDays(30)

    val existing = payRateRepository.findById(id)
      .orElseThrow { EntityNotFoundException("Pay rate with id '$id' not found") }

    require(request.startDate in today..maxAllowedStartDate) {
      "Pay rate start date must be today or in the next 30 days"
    }

    // If existing rate is in future, ensuring it cannot be updated
    require(existing.startDate <= today) { "Future pay rate must be cancelled before updating" }

    // If request start date is in future, ensuring only one future rate exists
    if (request.startDate > today &&
      payRateRepository.existsByPrisonCodeAndTypeAndStartDateAfter(existing.prisonCode, existing.type, today)
    ) {
      throw IllegalArgumentException("A future pay rate already exists")
    }

    return if (existing.startDate == today) {
      handleTodayRate(existing, request, currentUser)
    } else {
      createNewPayRate(existing, request, currentUser)
    }
  }

  private fun handleTodayRate(existing: PayRate, request: UpdatePayRateRequest, user: String): PayRateDto = if (request.startDate == existing.startDate) {
    existing.apply {
      rate = request.rate
      updatedBy = user
      updatedDateTime = LocalDateTime.now(clock)
    }.run { payRateRepository.save(this).toModel() }
  } else {
    createNewPayRate(existing, request, user)
  }

  private fun createNewPayRate(existing: PayRate, request: UpdatePayRateRequest, createdBy: String): PayRateDto {
    checkNoDuplicatesExist(prisonCode = existing.prisonCode, type = existing.type, startDate = request.startDate)

    return request.toEntity(
      prisonCode = existing.prisonCode,
      type = existing.type,
      createdBy = createdBy,
      clock = clock,
    ).run { payRateRepository.save(this) }.toModel()
  }

  private fun checkNoDuplicatesExist(prisonCode: String, type: PayStatusType, startDate: LocalDate) {
    require(!payRateRepository.existsByPrisonCodeAndTypeAndStartDate(prisonCode, type, startDate)) {
      "Pay rate already exists for prison: $prisonCode, type: $type on $startDate"
    }
  }
}
