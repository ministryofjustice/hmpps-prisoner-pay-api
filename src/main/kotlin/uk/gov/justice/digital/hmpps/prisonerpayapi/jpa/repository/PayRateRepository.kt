package uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.prisonerpayapi.jpa.entity.PayRate
import java.util.*

@Repository
interface PayRateRepository : JpaRepository<PayRate, UUID> {
  // Returns the latest rate (covers today or latest past rate) plus all future rates per prison
  @Query(
    """
    select pr from PayRate pr
    where pr.prisonCode = :prisonCode
    and (
        pr.startDate > current_date()
        or pr.startDate = (
            select max(pr2.startDate)
            from PayRate pr2
            where pr2.prisonCode = pr.prisonCode
            and pr2.type = pr.type
            and pr2.startDate <= current_date()
        )
    )
    order by pr.type, pr.startDate
""",
  )
  fun getCurrentAndFuturePayRatesByPrisonCode(prisonCode: String): List<PayRate>
}
