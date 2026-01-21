package uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayStatusPeriod
import java.time.LocalDate
import java.util.*

@Repository
interface PayStatusPeriodRepository : JpaRepository<PayStatusPeriod, UUID> {
  @Query(
    """
    select psp from PayStatusPeriod psp 
    where psp.startDate <= :latestStartDate
    and (:prisonCode is null or psp.prisonCode = :prisonCode)
    and (:activeOnly = false or psp.endDate is null or psp.endDate >= current_date())
    order by psp.startDate
  """,
  )
  fun search(latestStartDate: LocalDate, activeOnly: Boolean, prisonCode: String? = null): List<PayStatusPeriod>

  @Query(
    """
      select psp from PayStatusPeriod psp 
      where psp.prisonCode = :prisonCode
      and psp.startDate <= :date
      and (psp.endDate is null or psp.endDate >= :date)
      order by psp.prisonerNumber
    """,
  )
  fun findByPrisonCodeAndDate(prisonCode: String, date: LocalDate): List<PayStatusPeriod>
}
