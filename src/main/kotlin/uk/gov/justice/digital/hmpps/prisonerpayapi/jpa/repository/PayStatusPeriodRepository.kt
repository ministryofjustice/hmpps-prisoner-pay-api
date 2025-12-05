package uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusPeriod
import java.util.UUID

@Repository
interface PayStatusPeriodRepository : JpaRepository<PayStatusPeriod, UUID>
