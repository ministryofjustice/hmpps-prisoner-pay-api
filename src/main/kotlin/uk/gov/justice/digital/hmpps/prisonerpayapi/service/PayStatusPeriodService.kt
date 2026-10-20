package uk.gov.justice.digital.hmpps.prisonerpayapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prisonerpayapi.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request.CreatePayStatusPeriodRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request.UpdatePayStatusPeriodRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.response.PayStatusPeriod
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.repository.PayStatusPeriodRepository
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.prisonerpayapi.mapping.toEntity
import uk.gov.justice.digital.hmpps.prisonerpayapi.mapping.toModel
import uk.gov.justice.hmpps.kotlin.auth.HmppsAuthenticationHolder
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

@Service
@Transactional
class PayStatusPeriodService(
  private val authenticationHolder: HmppsAuthenticationHolder,
  private val repository: PayStatusPeriodRepository,
  private val updateService: PayStatusPeriodUpdateService,
  private val clock: Clock,
) {
  fun getById(id: UUID) = repository
    .findById(id)
    .orElseThrow { EntityNotFoundException("Pay Period Status with id '$id' not found") }
    .toModel()

  fun create(request: CreatePayStatusPeriodRequest): PayStatusPeriod {
    require(!request.startDate.isBefore(LocalDate.now(clock))) {
      "The start date must not be in the past"
    }
    require(request.endDate == null || !request.endDate.isBefore(LocalDate.now(clock))) {
      "The end date must not be in the past"
    }
    require(request.endDate == null || !request.endDate.isBefore(request.startDate)) {
      "The end date must not be before the start date"
    }

    return request
      // TODO: Should username be mandatory?
      .toEntity(authenticationHolder.username ?: SYSTEM_USERNAME, clock)
      .let { entity ->
        repository.save(entity)
      }
      .toModel()
  }

  @Transactional(readOnly = true)
  fun search(latestStartDate: LocalDate, activeOnly: Boolean, prisonCode: String? = null) = repository
    .search(latestStartDate, activeOnly, prisonCode)
    .map { it.toModel() }

  fun update(id: UUID, request: UpdatePayStatusPeriodRequest): PayStatusPeriod = updateService.update(id, request)

  fun delete(id: UUID) {
    repository.findOrThrowNotFound(id)
      .apply {
        require(startDate.isAfter(LocalDate.now(clock))) {
          "Cannot delete a pay status period with a start date in the past or today"
        }
      }
      .let { repository.delete(it) }
  }
}
