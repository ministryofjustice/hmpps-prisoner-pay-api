package uk.gov.justice.digital.hmpps.prisonerpayapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prisonerpayapi.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request.CreatePayStatusPeriodRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request.UpdatePayStatusPeriodRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.response.PayStatusPeriod
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.repository.PayStatusPeriodRepository
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

  fun create(request: CreatePayStatusPeriodRequest) = request
    // TODO: Should username be mandatory?
    .toEntity(authenticationHolder.username ?: SYSTEM_USERNAME, clock)
    .let { entity ->
      repository.save(entity)
    }
    .toModel()

  @Transactional(readOnly = true)
  fun search(latestStartDate: LocalDate, activeOnly: Boolean, prisonCode: String? = null) = repository
    .search(latestStartDate, activeOnly, prisonCode)
    .map { it.toModel() }

  fun update(id: UUID, request: UpdatePayStatusPeriodRequest): PayStatusPeriod = updateService.update(id, request)
}
