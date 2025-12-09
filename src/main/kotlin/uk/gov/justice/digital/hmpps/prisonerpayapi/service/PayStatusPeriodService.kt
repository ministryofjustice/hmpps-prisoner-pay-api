package uk.gov.justice.digital.hmpps.prisonerpayapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.prisonerpayapi.SYSTEM_USERNAME
import uk.gov.justice.digital.hmpps.prisonerpayapi.dto.request.CreatePayStatusPeriodRequest
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.repository.PayStatusPeriodRepository
import uk.gov.justice.digital.hmpps.prisonerpayapi.mapping.toEntity
import uk.gov.justice.digital.hmpps.prisonerpayapi.mapping.toModel
import uk.gov.justice.hmpps.kotlin.auth.HmppsAuthenticationHolder
import java.time.Clock
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class PayStatusPeriodService(
  private val authenticationHolder: HmppsAuthenticationHolder,
  private val repository: PayStatusPeriodRepository,
  private val clock: Clock,
) {
  @Transactional
  fun create(request: CreatePayStatusPeriodRequest) = request
    // TODO: Should username be mandatory?
    .toEntity(authenticationHolder.username ?: SYSTEM_USERNAME, clock)
    .let { entity ->
      repository.save(entity)
    }
    .toModel()

  fun search(latestStartDate: LocalDate, activeOnly: Boolean) = repository
    .search(latestStartDate, activeOnly)
    .map { it.toModel() }
}
